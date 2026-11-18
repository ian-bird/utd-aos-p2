package utd.aos.p2.logging;

import java.io.Serializable;
import java.time.Instant;

public record Log(LogType type, int nodeId, int messageCount, Instant ts) implements Serializable{};
