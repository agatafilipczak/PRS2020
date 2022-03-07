package thread;

public class SeparateThread {

    public static void main(String[] args) {

        Concurrency thread1 = new Concurrency(1); // create an object of class Concurrent which is a thread

        thread1.start();
        System.out.println("Main loop");
    }

    static class Concurrency extends Thread { // this class is a thread in Java, elements in run method will be run on separate Thread

        private int loopNum;

        Concurrency(int loopNum) {
            this.loopNum = loopNum;
        }

        @Override // override method from superclass
        public void run() {

            for (int i = 1; i <= 5; i++) {

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    System.out.println(e);
                }
                System.out.println("Loop " + this.loopNum + ", Iteration: " + i);
            }
        }
    }
}


