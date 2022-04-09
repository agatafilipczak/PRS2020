package tools;

import org.apache.log4j.Logger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class CountDown {

    static Logger logger = Logger.getLogger(CountDown.class);

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);

        /*
        *  To jest nasz szefu, on zaklada blokade -> latch.await()
        *  i czeka, az wszyscy robole skoncza prace,
        *  jak skoncza to wstaje, dopija kaweczke
        *  zdejmuje blokade i fajrant
        * */
        Thread kierownik = new Thread(() -> {
            try {
                latch.await();
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.info(" 3 Robole skonczyli swoją prace, FAJRANT!");
        });

        /* to są nasi robole, jest ich 3, kazdy musi zrobic swoja czesc pracy
        *  jak zrobi, to wtedy sie odklikuje w systemie, ze skonczyl, i licznik latch sie dekrementuje
        *  jak latch == 0, to znaczy ze wszyscy skonczyli pracowac i moga powiadomic kierownika, ze koniec robotaju
        *  kierownik sie budzi i wypuszcza ich do domu
        * */
        ExecutorService robole = Executors.newFixedThreadPool(3);
        IntStream.rangeClosed(1, 3).forEach(it -> {
            robole.submit(() -> {
                logger.info("Working " + Thread.currentThread().getName());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                latch.countDown();
                logger.info("Work is done " + Thread.currentThread().getName());
            });
        });

        // bez tego program sie nie wyłączy!!
        robole.shutdown();

        kierownik.start();
        kierownik.join();

    }
}
