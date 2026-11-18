package utd.aos.p2.logging;

import java.io.Serializable;
import java.time.Instant;

public record Log(LogType type, int nodeId, Instant ts) implements Serializable{};
