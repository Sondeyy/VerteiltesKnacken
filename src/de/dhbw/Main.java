package de.dhbw;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {
    public static void main(String[] args) throws InterruptedException, UnknownHostException {

        InetAddress localhost_ip = InetAddress.getByName("localhost");

        int port_W1 = 25_000;
        int port_W2 = 24_000;
        int port_W3 = 23_000;
        int port_W4 = 22_000;
        int port_W5 = 21_000;

        int primeRange = 10000;

        int initialCalculationCount = primeRange * 20;

        // Initialize Workers

        Worker worker1 = new Worker(port_W1, primeRange, initialCalculationCount);
        Thread worker1Thread = new Thread(worker1);
        worker1Thread.setName("Worker 1");

        Worker worker2 = new Worker(port_W2, port_W1, localhost_ip, primeRange, initialCalculationCount);
        Thread worker2Thread = new Thread(worker2);
        worker2Thread.setName("Worker 2");

        Worker worker3 = new Worker(port_W3, port_W1, localhost_ip, primeRange, initialCalculationCount);
        Thread worker3Thread = new Thread(worker3);
        worker3Thread.setName("Worker 3");

        Worker worker4 = new Worker(port_W4, port_W2, localhost_ip, primeRange, initialCalculationCount);
        Thread worker4Thread = new Thread(worker4);
        worker4Thread.setName("Worker 4");

        Worker worker5 = new Worker(port_W5, port_W1, localhost_ip, primeRange, initialCalculationCount);
        Thread worker5Thread = new Thread(worker5);
        worker4Thread.setName("Worker 5");

        // init Client
        Client client = new Client(localhost_ip, 25000, localhost_ip );

        //client.setChiffre("2d80afa14a65a7bf26636f97c89b43d5"); // test
        //client.setChiffre("b4820013b07bf8513ee59a905039fb631203c8b38ca3d59b475b4e4e092d3979");// 100
        //client.setChiffre("55708f0326a16870b299f913984922c7b5b37725ce0f6670d963adc0dc3451c8"); // 1_000
        //client.setChiffre("a9fc180908ad5f60556fa42b3f76e30f48bcddfad906f312b6ca429f25cebbd0"); // 10_000
        client.setChiffre("80f7b3b84e8354b36386c6833fe5c113445ce74cd30a21236a5c70f5fdca7208"); // 100_000

        //client.setPublicKey("268342277565109549360836262560222031507"); // test
        //client.setPublicKey("298874689697528581074572362022003292763"); // 100
        //client.setPublicKey("249488851623337787855631201847950907117"); // 1_000
        //client.setPublicKey("237023640130486964288372516117459992717"); // 10_000
        client.setPublicKey("174351747363332207690026372465051206619"); // 100_000

        Thread clientThread = new Thread(client);
        clientThread.setName("Client");

        System.out.println("--------------- start T1 -------------");
        worker1Thread.start();
        Thread.sleep(400);
        System.out.println("--------------- start T2 -------------");
        worker2Thread.start();
        Thread.sleep(400);
        System.out.println("--------------- start T3 -------------");
        worker3Thread.start();
        Thread.sleep(400);
        System.out.println("--------------- start T4 -------------");
        worker4Thread.start();
        Thread.sleep(400);
        System.out.println("--------------- start T5 -------------");
        worker5Thread.start();
        Thread.sleep(400);
        System.out.println("------------- start Client ------------");
        clientThread.start();
        Thread.sleep(400);

        try {
            worker1Thread.join();
            worker2Thread.join();
            worker3Thread.join();
            worker4Thread.join();
            worker5Thread.join();
            clientThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
