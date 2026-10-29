package utd.aos.p2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import utd.aos.p2.mutex.Maekawa;
import utd.aos.p2.mutex.MaekawaStrategy;
import utd.aos.p2.mutex.MutexData;
import utd.aos.p2.mutex.Preemption;
import utd.aos.p2.pusher.Pusher;
import utd.aos.p2.pusher.SubscriberManager;

public class Test {
    public static void main(String[] args) throws Exception {
        Main.readConfigParams("./inputs/sample-input.txt");
        MaekawaTest();
    }

    private static void MaekawaTest() {
        // set up the channels and timers
        List<Pusher<MutexData>> inputChannels = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            inputChannels.add(new SubscriberManager<>());
        }

        var a = new Maekawa(0, inputChannels.get(0),
                Arrays.asList(inputChannels.get(1)), Arrays.asList(1), new Preemption());
        var b = new Maekawa(1, inputChannels.get(1),
                Arrays.asList(inputChannels.get(0), inputChannels.get(2)), 
                Arrays.asList(0, 2), new Preemption());
        var c = new Maekawa(2, inputChannels.get(2),
                Arrays.asList(inputChannels.get(1)), Arrays.asList(1), new Preemption());

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
