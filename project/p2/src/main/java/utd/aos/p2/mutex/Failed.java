package utd.aos.p2.mutex;

import java.util.Map;

public final class Failed implements MutexData {
    public int id;

    public Failed(int id) {
        this.id = id;
    }

    public String toString() {
        return Map.of("type", "Failed",
                "senderId", Integer.toString(id)).toString();
    }
}
