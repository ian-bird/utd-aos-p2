package utd.aos.p2.mutex;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

import utd.aos.p2.chan.BufferedChan;
import utd.aos.p2.clock.ScalarClock;
import utd.aos.p2.pusher.Pusher;
import utd.aos.p2.utils.Pair;

public class Maekawa2 implements Mutex {
    // these are pairs of timestamp, quorum members that have to complete before we
    // can run.
    private PriorityQueue<Request> q;
    private BufferedChan<MutexData> workQueue;
    private ExecutorService worker;
    private ScalarClock c;

    private Set<Integer> waitingFor;
    private Phaser p;
    private Map<Integer, Pusher<MutexData>> outputsById;
    private Optional<Request> granted;
    public final int id;
    private boolean failed;

    private boolean debugMode;
    private boolean inCritical;
    private boolean requesting;
    private int requestTimestamp;

    public String toString() {
        return "{\"q\": " + q.toString() +
                ", \"c\": " + c.toString() +
                ", \"waitingFor\": " + waitingFor.toString() +
                ", \"outputsById\": " + outputsById.toString() +
                ", \"granted\": " + granted.toString() +
                ", \"id\": " + id +
                ", \"failed\": " + failed +
                ", \"inCritical\": " + inCritical +
                "}";
    }

    public Maekawa2(int myId, Pusher<MutexData> incoming, List<Pusher<MutexData>> outgoing,
            List<Integer> quoremMemberIds) {
        this.q = new PriorityQueue<>((a, b) -> {
            int order = Integer.compare(a.timeStamp, b.timeStamp);
            return order != 0 ? order : Integer.compare(a.senderId, b.senderId);
        });
        this.workQueue = new BufferedChan<>();
        this.worker = Executors.newSingleThreadExecutor(
                Thread.ofVirtual().name("maekawa2-" + myId).factory());
        this.id = myId;

        this.c = new ScalarClock();

        this.waitingFor = new HashSet<>();
        this.p = new Phaser(1);

        this.outputsById = new HashMap<>();
        Pair.zip(quoremMemberIds, outgoing)
                .stream()
                .forEach((p) -> this.outputsById.put(p.getKey(), p.getValue()));

        this.granted = Optional.empty();
        this.failed = false;
        this.debugMode = false;
        this.inCritical = false;

        // Serialize messages and local transitions without inline network callbacks.
        incoming.registerCallback((in) -> worker.execute(() -> workQueue.push(in)));

        this.workQueue.registerCallback((in) -> {
            try {
                debugLog("%d received %s\n", myId, in.toString());

                switch (in) {
                    // Reject busies without queuing a request that will be retried.
                    case Request r:
                        c = c.receive(new ScalarClock(r.timeStamp)).send();
                        granted.ifPresentOrElse((grantedTo) -> {
                            debugLog("%d rejecting request %s\n", myId, r.toString());
                            outputsById.get(r.senderId).push(new Failed(myId, r.timeStamp));
                        }, () -> {
                            debugLog("%d granting %s\n", myId, r);
                            q.add(r);
                            grant(r);
                        });
                        break;

                    // not using inquire for maekawa2
                    case Inquire i:
                        throw new IllegalStateException("Unexpected inquiry from " + i.senderId);

                    // not using yield for maekawa2
                    case Yield y:
                        throw new IllegalStateException("Unexpected yield from " + y.id);

                    // for release, remove that item from the queue and send a grant to the process
                    // at the front
                    case Release r:
                        if (!q.isEmpty()) {
                            debugLog("received release, removing %d from %s\n", r.id, q.toString());
                        } else {
                            debugLog("%d queue is empty\n", this.id);
                        }
                        q.removeIf((qr) -> qr.senderId == r.id);
                        if (!q.isEmpty()) {
                            debugLog("received release, so granting to %d\n", q.peek().senderId);
                            grant(q.peek());
                        } else {
                            synchronized (this) {
                                granted = Optional.empty();
                            }
                        }
                        break;

                    // when we get a grant, remove the loan for that, and if its the last thing
                    // we're waiting for then
                    // we can enter the critical section
                    case Grant g:
                        if (!requesting || !waitingFor.remove(g.id)) {
                            throw new IllegalStateException("Unexpected grant from " + g.id);
                        }
                        if (waitingFor.isEmpty()) {
                            inCritical = true;
                            debugLog("%d received last grant\n", this.id);
                            debugLog("registered parties: %s\n", p.getRegisteredParties());
                            p.arrive();
                            debugLog("%d arrived at %s!\n", this.id, p.getArrivedParties());
                        } else {
                            requestNextGrant();
                        }

                        break;

                    // Release only acquired tokens, then retry the whole attempt.
                    case Failed f:
                        if (requesting && !inCritical && f.ts == requestTimestamp) {
                            this.failed = true;
                            this.requesting = false;
                            outputsById.keySet().stream()
                                    .filter((id) -> !waitingFor.contains(id))
                                    .forEach((id) -> workQueue.push(new SendReleaseTo(id)));
                            p.arrive();
                        }
                        break;

                    case SendReleaseTo srt:
                        outputsById.get(srt.where).push(new Release(this.id));
                }
            } finally {
                this.workQueue.pull();
            }
        });
    }

    @Override
    public void csEnter() {
        // loop to retry until we get the resource
        do {
            // setup
            CompletableFuture.runAsync(() -> {
                this.failed = false;
                this.requesting = true;
                this.waitingFor = new HashSet<>(outputsById.keySet());

                // only register if the arrive in the lambda will immediately unblock
                if (p.getRegisteredParties() <= p.getArrivedParties() + 1)
                    p.register();
                requestTimestamp = c.counter;
                c = c.send();

                if (waitingFor.isEmpty()) {
                    inCritical = true;
                    p.arrive();
                } else {
                    requestNextGrant();
                }
            }, worker).join();

            // block until acquired or the attempt fails
            debugLog("%d waiting to arrive.\n", this.id);
            p.arriveAndAwaitAdvance();
        } while (failed);
        debugLog("%d entered critical.\n", this.id);

    }

    @Override
    public void csLeave() {
        CompletableFuture.runAsync(() -> {
            debugLog("%d released critical flag.\n", this.id);
            inCritical = false;
            requesting = false;

            // and tell everyone in the quorum that we're done.
            outputsById.keySet().stream().forEach((id) -> {
                workQueue.push(new SendReleaseTo(id));
            });
        }, worker).join();
    }

    public void enableLogging() {
        debugMode = true;
    }

    // grant another request and do the bookkeeping
    synchronized private void grant(Request r) {
        granted = Optional.of(r);
        outputsById.get(r.senderId).push(new Grant(this.id));
    }

    // get the next grant, for when the queue isnt empty
    private void requestNextGrant() {
        int nextId = waitingFor.stream().min(Integer::compare).orElseThrow();
        outputsById.get(nextId).push(new Request(id, requestTimestamp));
    }

    private void debugLog(String format, Object... objs) {
        if (!debugMode)
            return;

        System.out.printf(format, objs);
    }
}
