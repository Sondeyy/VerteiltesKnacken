package de.dhbw;

public class Main {

    public static void main(String[] args) {
        // init client
        Client client = new Client();
        Thread clientThread = new Thread(client);

        Worker worker1 = new Worker();
        Thread worker1Thread = new Thread(worker1);

        Worker worker2 = new Worker();
        Thread worker2Thread = new Thread(worker2);

        clientThread.start();
        worker1Thread.start();
        worker2Thread.start();
        
        try {
            clientThread.join();
            worker1Thread.join();
            worker2Thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
