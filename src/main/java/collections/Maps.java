package collections;


import org.apache.commons.lang.time.StopWatch;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Maps {

    private static int TEST_NO_ITEMS = 400000;

    public static void main(String[] args) throws Exception {
        StopWatch stopWatch = new StopWatch();

        stopWatch.reset();
        stopWatch.start();
            randomReadSynchronizedMap();
        stopWatch.stop();
        System.out.println("randomReadSynchronizedMap time: " + stopWatch.getTime());

        stopWatch.reset();
        stopWatch.start();
            randomReadConcurrentHashMap();
        stopWatch.stop();
        System.out.println("randomReadConcurrentHashMap time: " + stopWatch.getTime());

        stopWatch.reset();
        stopWatch.start();
        randomWriteSynchronizedMap();
        stopWatch.stop();
        System.out.println("randomWriteSynchronizedMap time: " + stopWatch.getTime());

        stopWatch.reset();
        stopWatch.start();
        randomWriteConcurrentHashMap();
        stopWatch.stop();
        System.out.println("randomWriteConcurrentHashMap time: " + stopWatch.getTime());

        stopWatch.reset();
        stopWatch.start();
        randomReadAndWriteSynchronizedMap();
        stopWatch.stop();
        System.out.println("randomReadAndWriteSynchronizedMap time: " + stopWatch.getTime());

        stopWatch.reset();
        stopWatch.start();
        randomReadAndWriteConcurrentHashMap();
        stopWatch.stop();
        System.out.println("randomReadAndWriteSynchronizedMap time: " + stopWatch.getTime());
    }

    public static void randomReadAndWriteSynchronizedMap() {
        Map<String, Integer> map = Collections.synchronizedMap(new HashMap<String, Integer>());
        performReadAndWriteTest(map);
    }

    public static void randomReadAndWriteConcurrentHashMap() {
        Map<String, Integer> map = new ConcurrentHashMap<>();
        performReadAndWriteTest(map);
    }

    private static void performReadAndWriteTest(final Map<String, Integer> map) {
        for (int i = 0; i < TEST_NO_ITEMS; i++) {
            Integer randNumber = (int) Math.ceil(Math.random() * TEST_NO_ITEMS);
            map.get(String.valueOf(randNumber));
            map.put(String.valueOf(randNumber), randNumber);
        }
    }

    public static void randomWriteSynchronizedMap() {
        Map<String, Integer> map = Collections.synchronizedMap(new HashMap<String, Integer>());
        performWriteTest(map);
    }

    public static void randomWriteConcurrentHashMap() {
        Map<String, Integer> map = new ConcurrentHashMap<>();
        performWriteTest(map);
    }

    private static void performWriteTest(final Map<String, Integer> map) {
        for (int i = 0; i < TEST_NO_ITEMS; i++) {
            Integer randNumber = (int) Math.ceil(Math.random() * TEST_NO_ITEMS);
            map.put(String.valueOf(randNumber), randNumber);
        }
    }

    public static void randomReadSynchronizedMap() {
        Map<String, Integer> map = Collections.synchronizedMap(new HashMap<String, Integer>());
        performReadTest(map);
    }

    public static void randomReadConcurrentHashMap() {
        Map<String, Integer> map = new ConcurrentHashMap<>();
        performReadTest(map);
    }

    private static void performReadTest(final Map<String, Integer> map) {
        for (int i = 0; i < TEST_NO_ITEMS; i++) {
            Integer randNumber = (int) Math.ceil(Math.random() * TEST_NO_ITEMS);
            map.get(String.valueOf(randNumber));
        }
    }
}

