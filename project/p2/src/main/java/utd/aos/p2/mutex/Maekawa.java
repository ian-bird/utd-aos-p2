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

public class Maekawa implements Mutex {
    // these are pairs of timestamp, quorum members that have to complete before we
    // can run.
    private PriorityQueue<Request> q;
    private BufferedChan<MutexData> workQueue;
    private ExecutorService worker;
    private ScalarClock c;

    private Set<Integer> waitingFor;
    private Set<Integer> outstandingLoans;
    private Set<Integer> inquiries;
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
                ", \"outstandingLoans\": " + outstandingLoans.toString() +
                ", \"outputsById\": " + outputsById.toString() +
                ", \"granted\": " + granted.toString() +
                ", \"id\": " + id +
                ", \"failed\": " + failed +
                ", \"inCritical\": " + inCritical +
                "}";
    }

    public Maekawa(int myId, Pusher<MutexData> incoming, List<Pusher<MutexData>> outgoing,
            List<Integer> quoremMemberIds) {
        this.q = new PriorityQueue<>((a, b) -> {
            int order = Integer.compare(a.timeStamp, b.timeStamp);
            return order != 0 ? order : Integer.compare(a.senderId, b.senderId);
        });
        this.workQueue = new BufferedChan<>();
        this.worker = Executors.newSingleThreadExecutor(
                Thread.ofVirtual().name("maekawa-" + myId).factory());
        this.id = myId;

        this.c = new ScalarClock();

        this.waitingFor = new HashSet<>();
        this.outstandingLoans = new HashSet<>();
        this.inquiries = new HashSet<>();
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
                    // upon receiving request:
                    // when we do not have an outstanding grant:
                    // -> then send a grant to the sender of the request.
                    // when we have an outstanding grant and the request is of lower priority:
                    // -> queue the request and report that the vote is unavailable.
                    // when we have an outstanding grant and the request is of higher priority:
                    // -> queue the request and report that the vote is unavailable and also inquire
                    // the quorum member.
                    case Request r:
                        c = c.receive(new ScalarClock(r.timeStamp)).send();
                        q.add(r);
                        granted.ifPresentOrElse((grantedTo) -> {
                            debugLog("%d rejecting request %s\n", myId, r.toString());
                            outputsById.get(r.senderId).push(new Failed(myId, r.timeStamp));
                            if (q.comparator().compare(r, grantedTo) < 0) {
                                debugLog("%d inquiring about %s to %d\n", myId, r.toString(), grantedTo.senderId);
                                outputsById.get(grantedTo.senderId).push(new Inquire(myId, r));
                            }
                        }, () -> {
                            debugLog("%d granting %s\n", myId, r);
                            grant(r);
                        });
                        break;

                    // Defer inquiries until we fail, and then yield.
                    case Inquire i:
                        if (requesting && !inCritical && !waitingFor.contains(i.senderId)) {
                            inquiries.add(i.senderId);
                            if (failed || !outstandingLoans.isEmpty()) {
                                yieldGrant(i.senderId);
                            }
                        }

                        break;

                    // when we receive a yield then grant to the thing at the top of the queue.
                    case Yield y:
                        if (granted.isPresent() && granted.get().senderId == y.id && granted.get().timeStamp == y.ts) {
                            debugLog("received yield, so granting to %d\n", q.peek().senderId);
                            grant(q.peek());
                        }
                        break;

                    // for release, remove that item from the queue and send a grant to the process at the front
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

                    // when we get a grant, remove the loan for that, and if its the last thing we're waiting for then
                    // we can enter the critical section
                    case Grant g:
                        if (!requesting || !waitingFor.remove(g.id)) {
                            throw new IllegalStateException("Unexpected grant from " + g.id);
                        }
                        outstandingLoans.remove(g.id);
                        if (waitingFor.isEmpty()) {
                            inCritical = true;
                            debugLog("%d received last grant\n", this.id);
                            debugLog("registered parties: %s\n", p.getRegisteredParties());
                            p.arrive();
                            debugLog("%d arrived at %s!\n", this.id, p.getArrivedParties());
                        }

                        break;

                    // when we get a failure, if we're not in the critical section and the failure is for our most recent request,
                    // then fail and yield to everyone 
                    case Failed f:
                        if (requesting && !inCritical && f.ts == requestTimestamp) {
                            this.failed = true;
                            inquiries.stream().forEach(this::yieldGrant);
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
        // setup
        CompletableFuture.runAsync(() -> {
            this.failed = false;
            this.requesting = true;
            this.waitingFor = new HashSet<>(outputsById.keySet());
            this.outstandingLoans = new HashSet<>();
            this.inquiries.clear();

            // only register if the arrive in the lambda will immediately unblock
            if (p.getRegisteredParties() <= p.getArrivedParties() + 1)
                p.register();
            requestTimestamp = c.counter;
            c = c.send();

            outputsById.values().stream().forEach((o) -> {
                o.push(new Request(id, requestTimestamp));
            });
        }, worker).join();

        // block until acquired
        debugLog("%d waiting to arrive.\n", this.id);
        p.arriveAndAwaitAdvance();
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

    // yield to another process. This does all the bookkeeping needed.
    private void yieldGrant(int otherId) {
        debugLog("%d yielding from %d\n", id, otherId);
        inquiries.remove(otherId);
        outstandingLoans.add(otherId);
        waitingFor.add(otherId);
        outputsById.get(otherId).push(new Yield(id, requestTimestamp));
    }

    private void debugLog(String format, Object... objs) {
        if (!debugMode)
            return;

        System.out.printf(format, objs);
    }
}
