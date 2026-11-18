package utd.aos.p2;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import utd.aos.p2.Config.ConfigParams;
import utd.aos.p2.logging.*;
import utd.aos.p2.mutex.Maekawa;
import utd.aos.p2.mutex.MutexData;
import utd.aos.p2.pusher.Listener;
import utd.aos.p2.pusher.Pusher;
import utd.aos.p2.pusher.Sender;
import utd.aos.p2.utils.Attempt;
import utd.aos.p2.utils.Serialization;

public class Main {

	public static void main(String[] args) throws UnknownHostException, Exception {
		if (args.length != 1) {
			throw new IllegalArgumentException("Expected 1 arguments: [config_file: path]");
		}

		var params = readConfigParams(args[0]);

		// extract name from the system host name
		var myName = InetAddress.getLocalHost().getHostName().split("\\.")[0].split("-")[1];
		System.out.println(myName);

		for (int i = 10; i < 49; i++) {
			var iCopy = i;
			if (params.nodeConfigs().stream().noneMatch((r) -> r.hostName().equals("dc" + iCopy))) {
				if (myName.equals("dc" + i)) {
					startLogger(params, myName);
				} else {
					startNode(params, myName, "dc" + i);
				}

				break;
			}
		}
	}

	private static class SettableNum {
		public int n;

		public SettableNum(int v) {
			n = v;
		}

		public void setNum(int v) {
			n = v;
		}
	}

	public static void startLogger(ConfigParams params, String myName) throws IOException {
		var incoming = new Listener<>(9000, (s) -> Serialization.<Log>deserialize(s).get());

		var logger = new Logger("");

		logger.begin_logging();

		var n = new SettableNum(0);

		incoming.registerCallback((log) -> {
			int val;
			synchronized (n) {
				val = n.n;
			}

			if (val == params.globalParams().numNodes() * params.globalParams().numRequestsPerNode())
				System.exit(0);
			else {
				synchronized (n) {
					n.setNum(val + 1);
				}
			}

			switch (log.type()) {
				case Entry:
					logger.log_enter(log.nodeId(), log.ts());
					break;

				case Exit:
					logger.log_exit(log.nodeId(), log.ts());
					break;
			}
		});


		try {
		Thread.sleep(1000 * 60 * 60 * 24);
		} catch (Exception _e) {			
		}
	}

	public static void startNode(ConfigParams params, String myName, String loggerHost)
			throws UnknownHostException, Exception {

		// get the associated config
		var myConfig = params.nodeConfigs().stream().filter((row) -> row.hostName().equals(myName)).findFirst().get();

		// open the port to listen to
		var incoming = new Listener<>(myConfig.listenPort(), (s) -> Serialization.<MutexData>deserialize(s).get());

		// set up the ports to send to
		List<Pusher<MutexData>> outgoing = new ArrayList<>();

		for (var id : myConfig.quorumIDs()) {
			var p = params.nodeConfigs().stream().filter((row) -> row.nodeID() == id).findFirst().get();
			outgoing.add(Attempt.withRetries(10, () -> new Sender<MutexData>(
					InetAddress.getByName(p.hostName().concat(".utdallas.edu")),
					p.listenPort(),
					Serialization::serialize)));
		}

		var logger = Attempt.withRetries(10, () -> new Sender<Log>(
				InetAddress.getByName(loggerHost + ".utdallas.edu"),
				9000,
				Serialization::serialize));

		// create the maekawa node
		var mutex = new Maekawa(myConfig.nodeID(), incoming, outgoing, myConfig.quorumIDs());
		Consumer<Runnable> atomically = (code) -> {
			mutex.csEnter();
			try {
				code.run();
			} finally {
				mutex.csLeave();
			}
		};

		for (int i = 0; i < params.globalParams().numRequestsPerNode(); i++) {
			atomically.accept(() -> {
				// log entry
				logger.push(new Log(LogType.Entry, myConfig.nodeID(), Instant.now()));

				sleepFor(params.globalParams().meanCsExecutionTime());

				// log exit
				logger.push(new Log(LogType.Exit, myConfig.nodeID(), Instant.now()));
			});

			sleepFor(params.globalParams().meanInterRequestDelay());
		}
	}

	public static ConfigParams readConfigParams(String path) {
		Path configFile = Path.of(path);

		if (!Files.exists(configFile)) {
			throw new IllegalArgumentException("config_file must be a valid file path");
		}

		return Config.parseConfigFile(configFile);
	}

	/**
	 * @param expDistSampleTime Samples an exponential dist with mean of this value
	 *                          and sleeps that long
	 */
	private static void sleepFor(Duration expDistSampleTime) {
		long amount = (long) (ThreadLocalRandom.current().nextExponential() * expDistSampleTime.toMillis());

		System.out.println("Sleeping for " + amount + "ms");

		try {
			Thread.sleep(amount);
		} catch (InterruptedException e) {
		}
	}
}
