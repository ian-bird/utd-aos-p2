package utd.aos.p2.mutex;

public final class Inquire implements MutexData {
    public int senderId;
    public Request about;

    public Inquire(int senderId, Request about) {
        this.senderId = senderId;
        this.about = about;
    }
}
