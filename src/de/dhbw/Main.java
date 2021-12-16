package de.dhbw;

import de.dhbw.client.Client;
import de.dhbw.worker.Worker;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Main {
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

        // init Client
        Client client = new Client(localhost_ip, startPort, localhost_ip );
        Thread clientThread = new Thread(client);
        clientThread.setName("Client");

        if(primeRange == 100){
                client.setChiffre("b4820013b07bf8513ee59a905039fb631203c8b38ca3d59b475b4e4e092d3979");
                client.setPublicKey("298874689697528581074572362022003292763");
        }else if(primeRange == 1_000){
            client.setChiffre("55708f0326a16870b299f913984922c7b5b37725ce0f6670d963adc0dc3451c8");
            client.setPublicKey("249488851623337787855631201847950907117");
        }else if(primeRange == 10_000){
            client.setChiffre("a9fc180908ad5f60556fa42b3f76e30f48bcddfad906f312b6ca429f25cebbd0");
            client.setPublicKey("237023640130486964288372516117459992717");
        }else if(primeRange == 100_000){
            client.setChiffre("80f7b3b84e8354b36386c6833fe5c113445ce74cd30a21236a5c70f5fdca7208");
            client.setPublicKey("174351747363332207690026372465051206619");
        }else{
            Logger.log("Unknown value for prime range");
            return;
        }

        for (Thread workerThread : workerThreads) {
            System.out.println("--------------- start Worker -------------");
            workerThread.start();
            Thread.sleep(200);
        }

        // start client
        clientThread.start();

        try {
            for (Thread workerThread : workerThreads) {
                workerThread.join();
            }
            clientThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
