package utd.aos.p2.pusher;

import java.util.function.Consumer;

import utd.aos.p2.chan.BufferedChan;

public class Fifo<T> implements Pusher<T> {
    private SubscriberManager<T> subs;
    private BufferedChan<T> workQueue;

    public Fifo() {
        subs = new SubscriberManager<>();
        workQueue = new BufferedChan<>();

        workQueue.registerCallback((v)->{
            workQueue.pull();

            subs.push(v);
        });
    }

    @Override
    public void push(T v) {
        workQueue.push(v);
    }

    @Override
    public void registerCallback(Consumer<T> cb) {
        subs.registerCallback(cb);        
    }
    
}
