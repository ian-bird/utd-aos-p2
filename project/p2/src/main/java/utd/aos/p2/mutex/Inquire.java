package utd.aos.p2.mutex;

import java.util.Map;

public final class Inquire implements MutexData {
    public int senderId;
    public Request about;

    public Inquire(int senderId, Request about) {
        this.senderId = senderId;
        this.about = about;
    }

    public String toString() {
        return Map.of("type", "Inquire",
                "senderId", Integer.toString(senderId),
                "about", about.toString()).toString();
    }
}
