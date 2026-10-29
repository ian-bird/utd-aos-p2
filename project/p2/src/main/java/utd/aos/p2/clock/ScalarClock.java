package utd.aos.p2.clock;

import java.io.Serializable;

public class ScalarClock implements Serializable, Clock<ScalarClock> {
    protected int counter;

    public ScalarClock() {
        this.counter = 0;
    }

    public ScalarClock(int t) {
        this.counter = t;
    }

    @Override
    public ScalarClock receive(ScalarClock c) {
        return new ScalarClock(c.counter > counter ? c.counter : counter);
    }

    @Override
    public ScalarClock send() {
        return new ScalarClock(counter + 1);
    }

    @Override
    public boolean covers(ScalarClock c) {
        return counter == c.counter + 1;
    }

    @Override
    public Comparison compare(ScalarClock c) {
        if(c.counter == counter)
            return Comparison.EQUAL;

        if(c.counter < counter)
            return Comparison.AFTER;

        return Comparison.BEFORE;
    }
    
}
