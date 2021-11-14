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

        // Initialize Workers

        Worker worker1 = new Worker(port_W1, localhost_ip);
        Thread worker1Thread = new Thread(worker1);
        worker1Thread.setName("Worker 1");

        Worker worker2 = new Worker(port_W2, localhost_ip, port_W1, localhost_ip);
        Thread worker2Thread = new Thread(worker2);
        worker2Thread.setName("Worker 2");

        Worker worker3 = new Worker(port_W3, localhost_ip, port_W1, localhost_ip);
        Thread worker3Thread = new Thread(worker3);
        worker3Thread.setName("Worker 3");

        Worker worker4 = new Worker(port_W4, localhost_ip, port_W2, localhost_ip);
        Thread worker4Thread = new Thread(worker4);
        worker4Thread.setName("Worker 4");

        // init Client
        Client client = new Client(21000, localhost_ip, 25000, localhost_ip );
        client.setChiffre("2d80afa14a65a7bf26636f97c89b43d5");
        client.setPublicKey("268342277565109549360836262560222031507");
        Thread clientThread = new Thread(client);

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

        // DEBUGGING
        Thread.sleep(1000);
        System.out.println("---------- Connections ----------------");
        System.out.println("Worker1: ".concat(worker1.getConnections().toString()));
        System.out.println("Worker2: ".concat(worker2.getConnections().toString()));
        System.out.println("Worker3: ".concat(worker3.getConnections().toString()));
        System.out.println("Worker4: ".concat(worker4.getConnections().toString()));
        System.out.println("---------- Broadcast Test ---------------");

        Message test = new Message();
        test.setType(MessageType.HELLO);
        test.setPayload("TESSSTT");
        worker1.broadcast(test);
        worker2.broadcast(test);
        worker3.broadcast(test);
        worker4.broadcast(test);

        Thread.sleep(2000);

        System.out.println("-------- Killing all Threads ---------");

        worker1.close();
        worker2.close();
        worker3.close();
        worker4.close();
        
        try {
            // clientThread.join();
            worker1Thread.join();
            worker2Thread.join();
            worker3Thread.join();
            worker4Thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
