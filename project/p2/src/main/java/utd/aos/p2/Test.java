package utd.aos.p2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import utd.aos.p2.mutex.Maekawa;
import utd.aos.p2.mutex.MutexData;
import utd.aos.p2.mutex.Preemption;
import utd.aos.p2.pusher.Pusher;
import utd.aos.p2.pusher.SubscriberManager;

public class Test {
    public static void main(String[] args) throws Exception {

        Main.readConfigParams("./inputs/sample-input.txt");
        concurrentMaekawaTest();
    }

    public static void concurrentMaekawaTest() {
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

        CompletableFuture.runAsync(()->{
            a.csEnter();
            System.out.println("a entered.");
            try{
                Thread.sleep(100);
            }catch(Exception _e) {
            }
            a.csLeave();
            System.out.println("a left.");

            try{
                Thread.sleep(20);
            }catch(Exception _e) {
            }

            b.csEnter();
            System.out.println("b entered.");
            try{
                Thread.sleep(100);
            }catch(Exception _e) {
            }
            b.csLeave();
            System.out.println("b left.");
        });

        CompletableFuture.runAsync(()->{
            try{
                Thread.sleep(50);
            }catch(Exception _e) {
            }

            System.out.println("c trying to enter...");
            c.csEnter();
            System.out.println("c entered.");
            try{
                Thread.sleep(100);
            }catch(Exception _e) {
            }
            c.csLeave();
            System.out.println("c left.");
        });

        try{
                Thread.sleep(1000);
            }catch(Exception _e) {
            }        
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
