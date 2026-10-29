package utd.aos.p2.mutex;

public final class Request implements MutexData {
    public int senderId;
    public int timeStamp;

    public Request(int senderId, int timeStamp) {
        this.senderId = senderId;
        this.timeStamp = timeStamp;
    }
}
