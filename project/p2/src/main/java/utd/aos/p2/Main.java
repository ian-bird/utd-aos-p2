package utd.aos.p2;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import utd.aos.p2.Config.ConfigParams;
import utd.aos.p2.Config.NodeConfig;
import utd.aos.p2.mutex.Maekawa;
import utd.aos.p2.mutex.MutexData;
import utd.aos.p2.pusher.Pusher;
import utd.aos.p2.pusher.SubscriberManager;

public class Main {
	public static void main(String[] args) {
		// if (args.length != 1) {
		// throw new IllegalArgumentException("Expected 1 arguments: [config_file:
		// path]");
		// }

		// var params = readConfigParams(args[0]);
		// var params = readConfigParams("./inputs/sample-input.txt");
		var params = readConfigParams("./inputs/minimal-2nodes.txt");

		List<Pusher<MutexData>> inputChannels = new ArrayList<>();
		for (int i = 0; i < params.globalParams().numNodes(); i++) {
			inputChannels.add(new SubscriberManager<>());
		}

		List<Maekawa> nodes = new ArrayList<>();

		for (NodeConfig nodeConfig : params.nodeConfigs()) {
			nodes.add(new Maekawa(nodeConfig.nodeID(), inputChannels.get(nodeConfig.nodeID()),
					nodeConfig.quorumIDs().stream().filter((p) -> p != nodeConfig.nodeID()).map(inputChannels::get)
							.toList(),
					nodeConfig.quorumIDs().stream().filter((p) -> p != nodeConfig.nodeID()).toList()));
		}

		System.out.println("nodes parsed and initialized:");
		nodes.forEach((node) -> {
			System.out.println(node);
			node.enableLogging();
	});

		var logger = new Logger("");
		logger.begin_logging();

		for (var node : nodes) {
			CompletableFuture.runAsync(() -> {
				for (int i = 0; i < params.globalParams().numRequestsPerNode(); i++) {
					try {
						node.csEnter();
						System.out.printf("%d entered.\n", node.id);
						logger.log_enter(node.id, Instant.now());
						Thread.sleep(params.globalParams().meanCsExecutionTime().toMillis());
						logger.log_exit(node.id, Instant.now());
						node.csLeave();
						Thread.sleep(params.globalParams().meanInterRequestDelay().toMillis());
					} catch (InterruptedException _ie) {

					}
				}
			});
		}

		try {
			Thread.sleep(Duration.ofSeconds(5));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		logger.end_logging();
	}

	/// Also prints out what's read!
	public static ConfigParams readConfigParams(String path) {
		Path configFile = Path.of(path);

		if (!Files.exists(configFile)) {
			throw new IllegalArgumentException("config_file must be a valid file path");
		}

		return Config.parseConfigFile(configFile);
	}
}
