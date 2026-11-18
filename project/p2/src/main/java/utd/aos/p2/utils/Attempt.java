package utd.aos.p2.utils;

import java.util.concurrent.Callable;

public class Attempt {
    public static <T> T withRetries(int limit, Callable<T> toTry) throws Exception {
        var retries = 0;
        while(true) {
            try {
                return toTry.call();
            } catch (Exception e) {
                if(retries++ > limit)
                    throw e;
                
                try {
                    Thread.sleep(100);
                } catch (InterruptedException _ie) {
                }
            }
        }
    }
}
