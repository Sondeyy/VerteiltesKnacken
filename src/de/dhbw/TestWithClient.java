package de.dhbw;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class TestWithClient {
    public static void main(String[] args) throws UnknownHostException, InterruptedException {
        InetAddress localhost_ip = InetAddress.getByName("localhost");

        InetAddress connection_address = InetAddress.getByName("192.168.2.141");
        int connection_port = 25000;

        int port_W1 = 23000;
        int port_W2 = 22000;

        int primeRange = 10000;

        int initialCalculationCount = primeRange * 30;

        // Initialize Workers

        Worker worker1 = new Worker(port_W1, connection_port, connection_address, primeRange, initialCalculationCount);
        Thread worker1Thread = new Thread(worker1);
        worker1Thread.setName("Worker 1");

        Worker worker2 = new Worker(port_W2, port_W1, localhost_ip, primeRange, initialCalculationCount);
        Thread worker2Thread = new Thread(worker2);
        worker2Thread.setName("Worker 2");

        // init Client
        Client client = new Client(21000, localhost_ip, 23000, localhost_ip );

        //client.setChiffre("2d80afa14a65a7bf26636f97c89b43d5"); // test
        //client.setChiffre("b4820013b07bf8513ee59a905039fb631203c8b38ca3d59b475b4e4e092d3979");// 100
        //client.setChiffre("55708f0326a16870b299f913984922c7b5b37725ce0f6670d963adc0dc3451c8"); // 1000
        client.setChiffre("a9fc180908ad5f60556fa42b3f76e30f48bcddfad906f312b6ca429f25cebbd0"); // 10000

        //client.setPublicKey("268342277565109549360836262560222031507"); // test
        //client.setPublicKey("298874689697528581074572362022003292763"); // 100
        //client.setPublicKey("249488851623337787855631201847950907117"); // 1000
        client.setPublicKey("237023640130486964288372516117459992717"); // 10000

        Thread clientThread = new Thread(client);
        clientThread.setName("Client");

        System.out.println("--------------- start T1 -------------");
        worker1Thread.start();
        Thread.sleep(400);
        System.out.println("--------------- start T2 -------------");
        worker2Thread.start();
        Thread.sleep(400);
        System.out.println("------------- start Client ------------");
        clientThread.start();
        Thread.sleep(400);

        try {
            worker1Thread.join();
            worker2Thread.join();
            clientThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
