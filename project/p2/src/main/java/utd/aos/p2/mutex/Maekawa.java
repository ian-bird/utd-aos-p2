package utd.aos.p2.mutex;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
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
    private ScalarClock c;

    private Set<Integer> waitingFor;
    private Set<Integer> outstandingLoans;
    private Phaser p;
    private Map<Integer, Pusher<MutexData>> outputsById;
    private Optional<Request> granted;
    private int id;
    private boolean failed;

    private boolean debugMode;
    

    public Maekawa(int myId, Pusher<MutexData> incoming, List<Pusher<MutexData>> outgoing,
            List<Integer> quoremMemberIds, MaekawaStrategy deadlockResolution) {
        this.q = new PriorityQueue<>((a, b) -> Integer.compare(a.timeStamp, b.timeStamp));
        this.workQueue = new BufferedChan<>();
        this.id = myId;

        this.c = new ScalarClock();

        this.waitingFor = new HashSet<>();
        this.outstandingLoans = new HashSet<>();
        this.p = new Phaser();

        this.outputsById = new HashMap<>();
        Pair.zip(quoremMemberIds, outgoing)
                .stream()
                .forEach((p) -> this.outputsById.put(p.getKey(), p.getValue()));

        this.granted = Optional.empty();
        this.id = myId;
        this.failed = false;
        this.debugMode = false;

        incoming.registerCallback(workQueue::push);

        this.workQueue.registerCallback((in) -> {
            this.workQueue.pull();

            debugLog("%d received %s\n", myId, in.toString());

            switch (in) {
                // upon receiving request: if we do not have an outstanding grant:
                // then send a grant to the sender of the request.
                // otherwise, if the outstanding grant is higher priority:
                // then send a failed to the sender of the request, and queue the request.
                // otherwise, send an inquire message to the process we've granted to.
                case Request r:

                    granted.ifPresentOrElse((grantedTo) -> {
                        if (grantedTo.timeStamp < r.timeStamp) {
                            debugLog("%d rejecting request %s\n", myId, r.toString());
                            q.add(r);
                            outputsById.get(r.senderId).push(new Failed(myId));
                        } else {
                            debugLog("%d inquiring about %s to %d\n", myId, r.toString(), grantedTo.senderId);
                            outputsById.get(grantedTo.senderId).push(new Inquire(myId, r));
                        }
                    }, () -> {
                        debugLog("%d granting %s\n",myId, r);
                        grant(r);
                    });
                    break;
                case Inquire i:
                    if (failed || !outstandingLoans.isEmpty()) {
                        debugLog("inquiry successful. Yielding\n");
                        outstandingLoans.add(i.senderId);
                        outputsById.get(i.senderId).push(new Yield(i.about.senderId, i.about.timeStamp));
                    }

                    break;
                // upon receiving yield: send a grant to the process at the front of the queue.
                // place the incoming request into the queue.
                case Yield y:
                    if (!q.isEmpty()) {
                        debugLog("received yield, so granting to %d\n", q.peek().senderId);
                        grant(q.peek());
                    }
                    q.add(new Request(y.id, y.ts));
                    break;
                // upon receiving release: remove that item from the queue
                // and send a grant to the process at the front of the queue.
                case Release r:
                    q.removeIf((qr) -> qr.senderId == r.id);
                    if (!q.isEmpty()) {
                        debugLog("received release, so granting to %d\n", q.peek().senderId);
                        grant(q.peek());
                    }
                    break;

                case Grant g:
                    outstandingLoans.remove(g.id);
                    waitingFor.remove(g.id);
                    if (waitingFor.isEmpty()) {
                        debugLog("received last grant\n");
                        p.arrive();
                    }

                    break;

                case Failed f:
                    this.failed = true;
                    break;
            }
        });

    }

    @Override
    public void csEnter() {
        // setup
        synchronized (this) {
            this.granted = Optional.empty();
            this.failed = false;

            this.waitingFor = outputsById.keySet();
            this.outstandingLoans = new HashSet<>();

            p.register();

            outputsById.values().stream().forEach((o) -> {
                o.push(new Request(id, c.counter));
            });
            c = c.send();
        }

        // block until done
        p.arriveAndAwaitAdvance();
    }

    @Override
    public void csLeave() {
        // tell everyone in the quorum that we're done
        outputsById.values().stream().forEach((o) -> o.push(new Release(id)));
    }

    public void enableLogging() {
        debugMode = true;
    }

    private void grant(Request r) {
        outputsById.get(r.senderId).push(new Grant(r.senderId));
        granted = Optional.of(r);
    }

    private void debugLog(String format, Object... objs) {
        if(!debugMode)
            return;

        System.out.printf(format, objs);
    }
}
