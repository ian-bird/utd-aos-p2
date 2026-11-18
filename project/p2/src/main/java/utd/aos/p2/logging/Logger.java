package utd.aos.p2.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import utd.aos.p2.Config.GlobalParams;



/**
 * Outputs a CSV to
 * {@code logger_outputs/<timestamp>_<logFileTitleNotes>_log.csv}
 *
 * <p>
 * Example:
 * 
 * <pre>
 * node_id,event,logger_time,node_time
 * 0,enter,2026-11-16T20:00:00Z,2026-11-16T19:59:59Z
 * 0,exit,2026-11-16T20:00:01Z,2026-11-16T20:00:00Z
 * 1,enter,2026-11-16T20:00:02Z,2026-11-16T20:00:01Z
 * 1,exit,2026-11-16T20:00:03Z,2026-11-16T20:00:02Z
 * </pre>
 *
 * <p>
 * Note:
 * {@code logger_time} is when the logger node received the event,
 * while {@code node_time} is when the node sent the event.
 */
public class Logger {
    Path logFilePath;
    private List<String> content;

    /**
     * The logfile path will be something like:
     * {@code logger_outputs/2026-11-17T03:55:07.343700Z_n=9_d=20_c=10_Maekawa1_sample-input_log.csv}.
     * 
     * @param gp            Pass in {@code params.globalParams()}; Logger will get
     *                      n, d, and c from it
     * @param isMaekawa2    True if you're using Maekawa2; false for Maekawa1.
     * @param inputFileName Name of the input config file (ex.: {@code
     *                      sample-input.txt}). Used for grouping logs in the
     *                      analyzer/graphing script.
     */
    public Logger(GlobalParams gp, boolean isMaekawa2, String inputFileName) {
        // The input file name might be something like "inputs/sample-input.txt"...
        // Clean it up to something like "sample-input"
        String cleanedInputFileName = Path.of(inputFileName).getFileName().toString();
        cleanedInputFileName = cleanedInputFileName.substring(0, cleanedInputFileName.lastIndexOf('.'));

        this.logFilePath = Path.of("logger_outputs/" +
                Instant.now() +
                "_n=" + gp.numNodes() +
                "_d=" + gp.meanInterRequestDelay() +
                "_c=" + gp.meanCsExecutionTime() +
                "_" + (isMaekawa2 ? "Maekawa2" : "Maekawa1") +
                "_" + cleanedInputFileName +
                "_log.csv");

        this.content = new ArrayList<>();
    }

    public void begin_logging() {
        content.add("node_id, event, logger_time, node_time");
    }

    public void log_enter(int nodeId, Instant ts) {
        // content.add("Node " + nodeId + " entered critical section at its time: " + ts
        // + "; local Logger node time is " + Instant.now());
        content.add(nodeId + ", enter, " + Instant.now() + ", " + ts);
    }

    public void log_exit(int nodeId, Instant ts) {
        // content.add("Node " + nodeId + " exited critical section at its time: " + ts
        // + "; local Logger node time is " + Instant.now());
        content.add(nodeId + ", exit, " + Instant.now() + ", " + ts);
    }

    public void end_logging() {
        try {
            Files.write(logFilePath, content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}