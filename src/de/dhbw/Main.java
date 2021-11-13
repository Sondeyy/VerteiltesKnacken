package de.dhbw;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {

    public static void main(String[] args) throws InterruptedException, UnknownHostException {
        // init client
        //Client client = new Client();
        //Thread clientThread = new Thread(client);
        InetAddress localhost_ip = InetAddress.getByName("localhost");

        int port_W1 = 25000;
        int port_W2 = 24000;
        int port_W3 = 23000;

        Worker worker1 = new Worker(port_W1, localhost_ip);
        Thread worker1Thread = new Thread(worker1);
        worker1Thread.setName("Worker 1");

        Worker worker2 = new Worker(port_W2, localhost_ip, port_W1, localhost_ip);
        Thread worker2Thread = new Thread(worker2);
        worker2Thread.setName("Worker 2");

        Worker worker3 = new Worker(port_W3, localhost_ip, port_W1, localhost_ip);
        Thread worker3Thread = new Thread(worker3);
        worker3Thread.setName("Worker 3");

        System.out.println("--------------- start T1 -------------");
        worker1Thread.start();
        Thread.sleep(1000);
        System.out.println("--------------- start T2 -------------");
        worker2Thread.start();
        Thread.sleep(1000);
        System.out.println("--------------- start T3 -------------");
        worker3Thread.start();
        Thread.sleep(100);
        //clientThread.start();

        // DEBUGGING
        Thread.sleep(2500);
        System.out.println("---------- Connections ----------------");
        System.out.println("Worker1: ".concat(worker1.getConnections().toString()));
        System.out.println("Worker2: ".concat(worker2.getConnections().toString()));
        System.out.println("Worker3: ".concat(worker3.getConnections().toString()));
        System.out.println("---------- Broadcast Test ---------------");

        Message test = new Message();
        test.setType(MessageType.HELLO);
        test.setPayload("TESSSTT");
        worker1.broadcast(test);
        worker2.broadcast(test);
        worker3.broadcast(test);
        
        try {
            // clientThread.join();
            worker1Thread.join();
            worker2Thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
