package de.dhbw;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class TestWithoutClient {
    public static void main(String[] args) throws InterruptedException, UnknownHostException {
        InetAddress localhost_ip = InetAddress.getByName("192.168.172.1");
        // TODO change ip

        int port_W1 = 25000;
        int port_W2 = 24000;

        int primeRange = 10000;

        int initialCalculationCount = primeRange * 20;

        // Initialize Workers

        Worker worker1 = new Worker(port_W1, localhost_ip, primeRange, initialCalculationCount);
        Thread worker1Thread = new Thread(worker1);
        worker1Thread.setName("Worker 1");

        Worker worker2 = new Worker(port_W2, localhost_ip, port_W1, localhost_ip, primeRange, initialCalculationCount);
        Thread worker2Thread = new Thread(worker2);
        worker2Thread.setName("Worker 2");

        System.out.println("--------------- start T1 -------------");
        worker1Thread.start();
        Thread.sleep(400);
        System.out.println("--------------- start T2 -------------");
        worker2Thread.start();
        Thread.sleep(400);

        try {
            worker1Thread.join();
            worker2Thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
