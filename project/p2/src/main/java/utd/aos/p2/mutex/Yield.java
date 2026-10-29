package utd.aos.p2.mutex;

final public class Yield implements MutexData {
    public int id;
    public int ts;

    public Yield(int id, int ts) {
        this.id = id;
        this.ts = ts;
    }
}
