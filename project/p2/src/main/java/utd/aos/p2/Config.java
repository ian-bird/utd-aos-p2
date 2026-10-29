package utd.aos.p2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Config {
    public static Config.ConfigParams parseConfigFile(Path configFilePath) {
        ArrayList<String> lines = getAllValidLines(configFilePath);

        String firstLine = lines.removeFirst();
        GlobalParams globalParams = GlobalParams.parse(firstLine);

        int numNodes = globalParams.numNodes();
        // System.out.println("\nGlobal parameters:\n\t" + globalParams);

        // Next n valid lines contain 3 tokens each: nodeID, hostName, listenPort
        ArrayList<NodeConfig> nodes = lines.stream().limit(numNodes).map(NodeConfig::parse)
                .collect(Collectors.toCollection(ArrayList::new));
        // System.out.println("\nNode lines: ");
        // nodes.forEach((s) -> System.out.println("\t" + s));

        // Then, a space delimited list of neighbors (up to n-1 tokens per line) for
        // each node;
        // one line per node, in order of nodeID
        var neighborLines = lines.stream().skip(numNodes).limit(numNodes).toList();

        // System.out.println("\nNeighbors: ");
        // neighborLines.forEach((s) -> System.out.println("\t" + s));

        for (int nodeID = 0; nodeID < numNodes; nodeID++) {
            // String line = lines.get(numNodes + nodeID);
            String line = neighborLines.get(nodeID);
            // truncate until first comment to ignore comments
            line = line.split("#")[0].trim();
            var parts = List.of(line.split(" "));

            var neighbors = parts.stream().map(Integer::parseInt).toList();
            nodes.get(nodeID).quorumIDs.addAll(neighbors);
        }

        // System.out.println("\nNode configs with neighbors: ");
        // nodes.forEach((s) -> System.out.println("\t" + s));

        return new ConfigParams(globalParams, nodes);
    }

    private static ArrayList<String> getAllValidLines(Path configFilePath) {
        try {
            return Files.readAllLines(configFilePath).stream()
                    .filter(line -> !line.isBlank() && !line.startsWith("#"))
                    .map(String::trim)
                    .collect(Collectors.toCollection(ArrayList::new));
        } catch (IOException e) {
            throw new RuntimeException("Error parsing config file: " + configFilePath, e);
        }
    }

    /// The first valid line of the configuration file contains four tokens.
    // The first token is the number of nodes in the system.
    // The second token is the mean value for inter-request delay (in milliseconds).
    // The third token is the mean value for cs-execution time (in milliseconds).
    // The fourth token is the number of requests each node should generate.
    record GlobalParams(int numNodes, Duration meanInterRequestDelay, Duration meanCsExecutionTime,
            int numRequestsPerNode) {
        public static GlobalParams parse(String line) throws NumberFormatException {
            String[] parts = line.split("\\s+");

            if (parts.length != 4) {
                throw new IllegalArgumentException("Expected 4 integers in the first line of the config file");
            }

            // Note: can throw NumberFormatException from parseInt
            int[] values = java.util.Arrays.stream(parts)
                    .mapToInt(Integer::parseInt)
                    .toArray();

            return new GlobalParams(values[0], Duration.ofMillis(values[1]), Duration.ofMillis(values[2]), values[3]);
        }
    }

    /// Creates it with the quorumIDs uninitialized (you add to them later on)
    record NodeConfig(int nodeID, String hostName, int listenPort, ArrayList<Integer> quorumIDs) {
        public static NodeConfig parse(String line) {
            // truncate to 3 elems to ignore comment
            String[] parts = java.util.Arrays.copyOf(line.split("\\s+"), 3);

            if (parts.length != 3) {
                throw new IllegalArgumentException("Expected 3 tokens in node config line");
            }

            int nodeID = Integer.parseInt(parts[0]);
            String hostName = parts[1];
            int listenPort = Integer.parseInt(parts[2]);

            return new NodeConfig(nodeID, hostName, listenPort, new ArrayList<>());
        }
    }

    record ConfigParams(GlobalParams globalParams, List<NodeConfig> nodeConfigs) {
    }
}
