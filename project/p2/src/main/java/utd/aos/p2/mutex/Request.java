package utd.aos.p2.mutex;

import java.util.Map;

public final class Request implements MutexData {
    public int senderId;
    public int timeStamp;

    public Request(int senderId, int timeStamp) {
        this.senderId = senderId;
        this.timeStamp = timeStamp;
    }

    public String toString() {
        return Map.of("type", "Request",
                "senderId", Integer.toString(senderId),
                "timeStamp", Integer.toString(timeStamp)).toString();
    }
}
