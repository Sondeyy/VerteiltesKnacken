package de.dhbw;

import de.dhbw.client.Client;
import de.dhbw.worker.Worker;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Properties;

public class ConsoleVersion {
    public static void main(String[] args) throws UnknownHostException {
        Properties props = new Properties();

        String fileName;
        if (args.length > 0) fileName = args[0];
        else fileName = "VerteiltesKnacken.conf";

        try (FileInputStream fis = new FileInputStream(fileName)) {
            props.load(fis);
        } catch (FileNotFoundException ex) {
            System.out.println("File not found!");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.out.println(props);

        InetAddress localhost_ip = InetAddress.getByName("localhost");

        int initialCalculationCount = Integer.parseInt(props.getProperty("primeRange")) * 30;

        ArrayList<Thread> threads = new ArrayList<>();

        for (int i = 0; i < Integer.parseInt(props.getProperty("workerThreads")); i++) {
            Worker worker;
            if (i == 0 && props.getProperty("client").equals("no")) {
                System.out.println("Creating first Worker");
                worker = new Worker(
                        Integer.parseInt(props.getProperty("myPort")),
                        Integer.parseInt(props.getProperty("primeRange")),
                        initialCalculationCount);
            } else {
                System.out.println("Creating Worker ".concat(String.valueOf(i)));
                worker = new Worker(
                        Integer.parseInt(props.getProperty("myPort")) + i,
                        Integer.parseInt(props.getProperty("connectionPort")),
                        InetAddress.getByName(props.getProperty("connectionAddress")),
                        Integer.parseInt(props.getProperty("primeRange")),
                        initialCalculationCount);
            }

            Thread workerThread = new Thread(worker);
            workerThread.setName("Worker ".concat(String.valueOf(i)));
            threads.add(workerThread);
        }

        if (props.getProperty("client").equals("yes")) {
            System.out.println("Creating Client");
            Client client = new Client(
                    localhost_ip,
                    Integer.parseInt(props.getProperty("connectionPort")),
                    InetAddress.getByName(props.getProperty("connectionAddress"))
            );
            switch (props.getProperty("primeRange")) {
                case "100" -> {
                    client.setChiffre("b4820013b07bf8513ee59a905039fb631203c8b38ca3d59b475b4e4e092d3979");// 100
                    client.setPublicKey("298874689697528581074572362022003292763"); // 100
                }
                case "1000" -> {
                    client.setPublicKey("249488851623337787855631201847950907117"); // 1000
                    client.setChiffre("55708f0326a16870b299f913984922c7b5b37725ce0f6670d963adc0dc3451c8"); // 1000
                }
                case "10000" -> {
                    client.setPublicKey("237023640130486964288372516117459992717"); // 10000
                    client.setChiffre("a9fc180908ad5f60556fa42b3f76e30f48bcddfad906f312b6ca429f25cebbd0"); // 10000
                }
                case "100000" -> {
                    client.setPublicKey("174351747363332207690026372465051206619"); // 100000
                    client.setChiffre("80f7b3b84e8354b36386c6833fe5c113445ce74cd30a21236a5c70f5fdca7208"); // 100000
                }
            }
            Thread clientThread = new Thread(client);
            threads.add(clientThread);
        }
        for (Thread thread : threads) {
            System.out.println("Starting ".concat(thread.getName()));
            thread.start();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for (Thread thread : threads) {
            System.out.println("Joining ".concat(thread.getName()));
            try {
                thread.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        System.out.println("Thank you for using VerteiltesKnacken!");
    }
}
