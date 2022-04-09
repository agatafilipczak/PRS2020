package tools;

import org.apache.log4j.Logger;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class Barrier {
    static Logger logger = Logger.getLogger(Barrier.class);

    public static void main(String[] args) throws InterruptedException {
        CyclicBarrier barrier = new CyclicBarrier(4);

        Thread thread = new Thread(() -> {
            logger.info("Bariera zalozona");
            try {
                logger.info("tescik");
                barrier.await();
                logger.info("Wszystkie zakonczyly swoje taski, moge dzialac");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }

            logger.info("I can process now");
        });

        ExecutorService service = Executors.newFixedThreadPool(3);
        IntStream.rangeClosed(1, 3).forEach(it -> {
            service.submit(() -> {
                try {
                    logger.info("WAIT " + Thread.currentThread().getName());
                    barrier.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                }
            });
        });

        thread.start();
        thread.join();
        service.shutdown();
    }

}
