package utd.aos.p2;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import utd.aos.p2.logging.Logger;
import utd.aos.p2.mutex.Maekawa;
import utd.aos.p2.mutex.Maekawa2;
import utd.aos.p2.mutex.Mutex;
import utd.aos.p2.mutex.MutexData;
import utd.aos.p2.pusher.Pusher;
import utd.aos.p2.pusher.SubscriberManager;

public class Test {
    public static void main(String[] args) throws Exception {

        Main.readConfigParams("./inputs/sample-input.txt");
        // concurrentMaekawaTest();

        loggedMaekawaTest();
    }

    public static Consumer<Runnable> atomically(Mutex m, Logger l, int id) {
        return (r) -> {
            m.csEnter();
            l.log_enter(id, Instant.now());
            try {
                r.run();
            } finally {
                l.log_exit(id, Instant.now());
                m.csLeave();
            }
        };
    }

    public static void loggedMaekawaTest() {
        // set up the channels and timers
        List<Pusher<MutexData>> inputChannels = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            inputChannels.add(new SubscriberManager<>());
        }

        // a = {0,1}
        var a = new Maekawa(0, inputChannels.get(0),
                Arrays.asList(inputChannels.get(1)), Arrays.asList(1));

        // b = {0, 1, 2}
        var b = new Maekawa(1, inputChannels.get(1),
                Arrays.asList(inputChannels.get(0)),
                Arrays.asList(0));

        var log = new Logger("");

        var ar = atomically(a, log, 0);
        var br = atomically(b, log, 1);

        log.begin_logging();

        CompletableFuture.runAsync(() -> {
            while (true) {
                ar.accept(() -> {
                    try {
                        Thread.sleep(71);
                    } catch (Exception _e) {
                    }
                });

                try {
                    Thread.sleep(29);
                } catch (Exception _e) {
                }

                br.accept(() -> {
                    try {
                        Thread.sleep(71);
                    } catch (Exception _e) {
                    }
                });

                try {
                    Thread.sleep(29);
                } catch (Exception _e) {
                }
            }
        });

        // CompletableFuture.runAsync(() -> {
        // while (true) {
        // try {
        // Thread.sleep(17);
        // } catch (Exception _e) {
        // }

        // cr.accept(() -> {
        // try {
        // Thread.sleep(41);
        // } catch (Exception _e) {
        // }

        // c.csLeave();
        // });
        // }
        // });

        try {
            Thread.sleep(1000);
        } catch (Exception _e) {
        }

        log.end_logging();
    }

    public static void concurrentMaekawaTest() {
        // set up the channels and timers
        List<Pusher<MutexData>> inputChannels = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            inputChannels.add(new SubscriberManager<>());
        }

        // a = {0,1}
        var a = new Maekawa2(0, inputChannels.get(0),
                Arrays.asList(inputChannels.get(1)), Arrays.asList(1));

        // b = {0, 1, 2}
        var b = new Maekawa2(1, inputChannels.get(1),
                Arrays.asList(inputChannels.get(0), inputChannels.get(2)),
                Arrays.asList(0, 2));

        // c = {1 , 2}
        var c = new Maekawa2(2, inputChannels.get(2),
                Arrays.asList(inputChannels.get(1)), Arrays.asList(1));

        // a.enableLogging();
        // b.enableLogging();
        // c.enableLogging();

        System.out.printf("0 = %s\n", a.toString());

        CompletableFuture.runAsync(() -> {
            while (true) {
                a.csEnter();
                System.out.println("0 entered.");
                try {
                    Thread.sleep(71);
                } catch (Exception _e) {
                }
                System.out.println("0 leaving...\n");
                a.csLeave();

                try {
                    Thread.sleep(29);
                } catch (Exception _e) {
                }

                b.csEnter();
                System.out.println("1 entered.");
                try {
                    Thread.sleep(71);
                } catch (Exception _e) {
                }
                System.out.println("1 leaving...\n");
                b.csLeave();

                try {
                    Thread.sleep(29);
                } catch (Exception _e) {
                }
            }
        });

        CompletableFuture.runAsync(() -> {
            while (true) {
                try {
                    Thread.sleep(17);
                } catch (Exception _e) {
                }

                c.csEnter();
                System.out.println("2 entered.");
                try {
                    Thread.sleep(41);
                } catch (Exception _e) {
                }
                System.out.println("2 leaving...\n");
                c.csLeave();
            }
        });

        try {
            Thread.sleep(1000);
        } catch (Exception _e) {
        }

        System.out.printf("0: %s\n", a.toString());
        System.out.printf("1: %s\n", b.toString());
        System.out.printf("2: %s\n", c.toString());

        while (true)
            ;
    }

    private static void MaekawaTest() {
        // set up the channels and timers
        List<Pusher<MutexData>> inputChannels = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            inputChannels.add(new SubscriberManager<>());
        }

        var a = new Maekawa(0, inputChannels.get(0),
                Arrays.asList(inputChannels.get(1)), Arrays.asList(1));
        var b = new Maekawa(1, inputChannels.get(1),
                Arrays.asList(inputChannels.get(0), inputChannels.get(2)),
                Arrays.asList(0, 2));
        var c = new Maekawa(2, inputChannels.get(2),
                Arrays.asList(inputChannels.get(1)), Arrays.asList(1));

        a.enableLogging();
        b.enableLogging();
        c.enableLogging();

        a.csEnter();
        System.out.println("a entered.");
        a.csLeave();
        System.out.println("a left.");

        b.csEnter();
        System.out.println("b entered.");
        System.out.println("c trying to enter, this should hang.");
        c.csEnter();
        b.csLeave();
        System.out.println("b left.");
        c.csLeave();
        System.out.println("c left. This should never happen.");
    }
}
