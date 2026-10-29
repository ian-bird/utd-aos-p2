package utd.aos.p2.mutex;

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

    public Maekawa(int myId, Pusher<MutexData> incoming, List<Pusher<MutexData>> outgoing,
            List<Integer> membershipSetIds, MaekawaStrategy deadlockResolution) {
        this.q = new PriorityQueue<>((a, b) -> Integer.compare(a.timeStamp, b.timeStamp));
        this.workQueue = new BufferedChan<>();
        this.id = myId;

        incoming.registerCallback(workQueue::push);

        this.workQueue.registerCallback((in) -> {
            this.workQueue.pull();

            switch (in) {
                // upon receiving request: if we do not have an outstanding grant:
                // then send a grant to the sender of the request.
                // otherwise, if the outstanding grant is higher priority:
                // then send a failed to the sender of the request, and queue the request.
                // otherwise, send an inquire message to the process we've granted to.
                case Request r:
                    granted.ifPresentOrElse((grantedTo) -> {
                        if (grantedTo.timeStamp < r.timeStamp) {
                            q.add(r);
                            outputsById.get(r.senderId).push(new Failed(myId));
                        } else {
                            outputsById.get(grantedTo.senderId).push(new Inquire(myId, r));
                        }
                    }, () -> {
                        grant(r);
                    });
                    break;
                case Inquire i:
                    if (failed || !outstandingLoans.isEmpty()) {
                        outstandingLoans.add(i.senderId);
                        outputsById.get(i.senderId).push(new Yield(i.about.senderId, i.about.timeStamp));
                    }

                    break;
                // upon receiving yield: send a grant to the process at the front of the queue.
                // place the incoming request into the queue.
                case Yield y:
                    if (!q.isEmpty())
                        grant(q.peek());
                    q.add(new Request(y.id, y.ts));
                    break;
                // upon receiving release: remove that item from the queue
                // and send a grant to the process at the front of the queue.
                case Release r:
                    q.removeIf((qr) -> qr.senderId == r.id);
                    if (!q.isEmpty())
                        grant(q.peek());
                    break;

                case Grant g:
                    outstandingLoans.remove(g.id);
                    waitingFor.remove(g.id);
                    if (waitingFor.isEmpty()) {
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

    private void grant(Request r) {
        outputsById.get(r.senderId).push(new Grant(r.senderId));
        granted = Optional.of(r);
    }

}
