package utd.aos.p2.mutex;

import java.util.Map;

public final class Failed implements MutexData {
    public int id;
    public int ts;

    public Failed(int id, int ts) {
        this.id = id;
        this.ts = ts;
    }

    public String toString() {
        return Map.of("type", "Failed",
                "senderId", Integer.toString(id),
                "ts", Integer.toString(ts)).toString();
    }
}
