package utd.aos.p2;

import java.nio.file.Files;
import java.nio.file.Path;

import utd.aos.p2.Config.ConfigParams;

public class Main {
	public static void main(String[] args) {
		if (args.length != 1) {
			throw new IllegalArgumentException("Expected 1 arguments: [config_file: path]");
		}

		readConfigParams(args[0]);
	}

	/// Also prints out what's read!
	public static void readConfigParams(String path) {
		Path configFile = Path.of(path);

		if (!Files.exists(configFile)) {
			throw new IllegalArgumentException("config_file must be a valid file path");
		}

		ConfigParams configParams = Config.parseConfigFile(configFile);

		System.out.println(configParams.globalParams());
		configParams.nodeConfigs().forEach(nc -> System.out.println(nc));
	}
}
