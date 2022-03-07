package thread;

public class SingleThread {
    public static void main(String[] args) { //main is a function in java that is run, only classes with main method can be executed in Java

        for (byte i = 1; i <= 5; i++) {

            // sleep stops thread for given ms

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            // write elements to screen
            System.out.println("Loop 1, Iteration: " + i);
        }

        for (int i = 1; i <= 5; i++) {

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                System.out.println(e);
            }
            System.out.println("Loop 2, Iteration: " + i);
        }
    }
}
