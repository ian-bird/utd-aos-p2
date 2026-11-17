package utd.aos.p2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Outputs a CSV to {@code logger_outputs/<timestamp>_<logFileTitleNotes>_log.csv}
 *
 * <p>Example:
 * <pre>
 * node_id,event,logger_time,node_time
 * 0,enter,2026-11-16T20:00:00Z,2026-11-16T19:59:59Z
 * 0,exit,2026-11-16T20:00:01Z,2026-11-16T20:00:00Z
 * 1,enter,2026-11-16T20:00:02Z,2026-11-16T20:00:01Z
 * 1,exit,2026-11-16T20:00:03Z,2026-11-16T20:00:02Z
 * </pre>
 *
 * <p>Note:
 * {@code logger_time} is when the logger node received the event,
 * while {@code node_time} is when the node sent the event.
 */
class Logger {
    Path logFilePath;
    private List<String> content;

    /**
     * The logfile path will be
     * `logger_outputs/<timestamp>_<logFileTitleNotes>_log.csv`.
     * 
     * @param logFileTitleNotes Optional, set to "" if you don't want anything.
     */
    public Logger(String logFileTitleNotes) {
        this.logFilePath = Path.of("logger_outputs/" + Instant.now() + "_" + logFileTitleNotes + "_log.csv");
        this.content = new ArrayList<>();
    }

    void begin_logging() {
        content.add("node_id, event, logger_time, node_time");
    }

    void log_enter(int nodeId, Instant ts) {
        // content.add("Node " + nodeId + " entered critical section at its time: " + ts
        // + "; local Logger node time is " + Instant.now());
        content.add(nodeId + ", enter, " + Instant.now() + ", " + ts);
    }

    void log_exit(int nodeId, Instant ts) {
        // content.add("Node " + nodeId + " exited critical section at its time: " + ts
        // + "; local Logger node time is " + Instant.now());
        content.add(nodeId + ", exit, " + Instant.now() + ", " + ts);
    }

    void end_logging() {
        try {
            Files.write(logFilePath, content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}