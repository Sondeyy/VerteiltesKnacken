package de.dhbw;

import de.dhbw.worker.Worker;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class TestWithoutClient {
    public static void main(String[] args) throws InterruptedException, UnknownHostException {
        InetAddress localhost_ip = InetAddress.getByName("localhost");

        int startPort = 25_000;

        int primeRange = 10000;
        int workers = 10;
        int initialCalculationCount = primeRange * 50;
        ArrayList<Thread> workerThreads = new ArrayList<>();

        // Initialize first worker
        Worker worker1 = new Worker(startPort, primeRange, initialCalculationCount);
        Thread worker1Thread = new Thread(worker1);
        worker1Thread.setName("Worker 1");
        workerThreads.add(worker1Thread);

        // initialize other workers
        for(int workerNr = 2; workerNr <= workers; workerNr++){
            Worker worker_n = new Worker(startPort-workerNr, primeRange, initialCalculationCount, startPort, localhost_ip);
            Thread workerNThread = new Thread(worker_n);
            workerNThread.setName("Worker ".concat(String.valueOf(workerNr)));
            workerThreads.add(workerNThread);
        }

        for (Thread workerThread : workerThreads) {
            System.out.println("--------------- start Worker -------------");
            workerThread.start();
            Thread.sleep(400);
        }

        try {
            for (Thread workerThread : workerThreads) {
                workerThread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
