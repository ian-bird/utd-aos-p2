package utd.aos.p2.mutex;

public interface Mutex {
    void csEnter();
    void csLeave();
}