package de.dhbw;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {

    public static void main(String[] args) throws InterruptedException, UnknownHostException {

        InetAddress localhost_ip = InetAddress.getByName("localhost");

        int port_W1 = 25000;
        int port_W2 = 24000;
        int port_W3 = 23000;
        int port_W4 = 22000;

        int primeRange = 100;

        int segmentSize = 200;

        // Initialize Workers

        Worker worker1 = new Worker(port_W1, localhost_ip, primeRange, segmentSize);
        Thread worker1Thread = new Thread(worker1);
        worker1Thread.setName("Worker 1");

        Worker worker2 = new Worker(port_W2, localhost_ip, port_W1, localhost_ip, primeRange, segmentSize);
        Thread worker2Thread = new Thread(worker2);
        worker2Thread.setName("Worker 2");

        Worker worker3 = new Worker(port_W3, localhost_ip, port_W1, localhost_ip, primeRange, segmentSize);
        Thread worker3Thread = new Thread(worker3);
        worker3Thread.setName("Worker 3");

        Worker worker4 = new Worker(port_W4, localhost_ip, port_W2, localhost_ip, primeRange, segmentSize);
        Thread worker4Thread = new Thread(worker4);
        worker4Thread.setName("Worker 4");

        // init Client
        Client client = new Client(21000, localhost_ip, 25000, localhost_ip );
        //client.setChiffre("2d80afa14a65a7bf26636f97c89b43d5");
        client.setChiffre("b4820013b07bf8513ee59a905039fb631203c8b38ca3d59b475b4e4e092d3979");
        //client.setPublicKey("268342277565109549360836262560222031507");
        client.setPublicKey("298874689697528581074572362022003292763");
        Thread clientThread = new Thread(client);
        clientThread.setName("Client");

        System.out.println("--------------- start T1 -------------");
        worker1Thread.start();
        Thread.sleep(200);
        System.out.println("--------------- start T2 -------------");
        worker2Thread.start();
        Thread.sleep(200);
        System.out.println("--------------- start T3 -------------");
        worker3Thread.start();
        Thread.sleep(200);
        System.out.println("--------------- start T4 -------------");
        worker4Thread.start();
        Thread.sleep(200);
        System.out.println("------------- start Client ------------");
        clientThread.start();
        Thread.sleep(200);

        try {
            // clientThread.join();
            worker1Thread.join();
            worker2Thread.join();
            worker3Thread.join();
            worker4Thread.join();
            clientThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
