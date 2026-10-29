package utd.aos.p2.mutex;

import java.util.Map;

public final class Release implements MutexData {
    public int id;

    public Release(int id) {
        this.id = id;
    }

    public String toString() {
        return Map.of("type", "Release",
                "senderId", Integer.toString(id)).toString();
    }
}
