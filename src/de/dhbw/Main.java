package de.dhbw;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        // init client
        Client client = new Client();
        Thread clientThread = new Thread(client);

        // create list of nodes in network
        List<Connection> network = new ArrayList<>();

        Worker worker1 = new Worker(1, network);
        Thread worker1Thread = new Thread(worker1);

        Worker worker2 = new Worker(2, network);
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
