package utd.aos.p2.mutex;

import java.util.Map;

public final class Grant implements MutexData {
    public int id;

    public Grant(int id) {
        this.id = id;
    }

    public String toString() {
        return Map.of("type", "Grant",
                "senderId", Integer.toString(id)).toString();
    }
}
