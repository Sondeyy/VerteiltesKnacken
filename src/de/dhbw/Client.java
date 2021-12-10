package de.dhbw;

import de.dhbw.examhelpers.rsa.RSAHelper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client implements Runnable {
    private final int listenerPort;
    private final InetAddress address;
    private final int clusterPort;
    private final InetAddress clusterAddress;
    private final AtomicBoolean active = new AtomicBoolean(true);
    private String chiffre;
    private String publicKey;
    private final int timeout = 500;

    private Connection clusterConnection;
    private ArrayList<WorkerInfo> clusterinfo;

    public Client(int listenerPort, InetAddress address, int clusterPort, InetAddress clusterAddress) {
        this.listenerPort = listenerPort;
        this.address = address;
        this.clusterPort = clusterPort;
        this.clusterAddress = clusterAddress;
    }

    /**
     * @param port port to connect to
     * @param address  host to connect to
     */
    public void connectTo(int port, InetAddress address) {
        try {
            Socket socket = new Socket(address, port);
            Connection connection = new Connection(socket);
            connection.connectStreamsClient();
            connection.setListenerPort(port);
            this.clusterConnection = connection;

            Logger.log(String.format("Connected to: %s:%d", address, port));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     */
    private void sendPublicKey(String publicKey) {
        if(clusterConnection != null){
            // create RSA message
            Message msg = new Message();
            RSAPayload payload = new RSAPayload(publicKey, listenerPort, address);
            msg.setPayload(payload);
            msg.setType(MessageType.RSA);

            // send payload to cluster
            this.clusterConnection.write(msg);
        } else {
            Logger.log("Connect to cluster before sending RSA Key!");
        }
    }

    private void sendHeartBeat(){
        Message heartBeat = new Message();
        heartBeat.setType(MessageType.ALIVE);

        // todo: Handle Fail
        this.clusterConnection.write(heartBeat);
    }

    private void reconnect(){
        // reconnect to one of the nodes in the cluster
        WorkerInfo newConnection = clusterinfo.get(0);
        Logger.log(String.format("Reconnection to: %s: %d", newConnection.address, newConnection.listenerPort));
        this.connectTo(newConnection.listenerPort, newConnection.address);
        Logger.log("Successful reconnect!");
    }

    @Override
    public void run() {

        // connect to cluster
        connectTo(clusterPort, clusterAddress);

        // send publicKEy to cluster
        sendPublicKey(publicKey);
        Instant startTime = Instant.now();

        // create decryption helper
        RSAHelper helper = new RSAHelper();

        // wait for cluster response
        while(active.get()){
            // todo: Handle Server disconnect
            if(clusterConnection.available()){
                Message answer = clusterConnection.read();

                if (answer.getType() == MessageType.ANSWER_FOUND) {

                    PrimeCalculationResult result = ((PrimeCalculationResult) answer.getPayload());

                    Logger.log("Received solution: ".concat(answer.toString()));
                    Logger.log("Percentage of segments: ".concat(String.valueOf(result.percentageCalculated)));
                    PrimeCalculationResult solution = (PrimeCalculationResult) answer.getPayload();

                    if(helper.isValid(solution.p,solution.q,publicKey)){
                        String decryptedText = helper.decrypt(solution.p, solution.q, chiffre);
                        Logger.log("Decrypted Chiffre is: ".concat(decryptedText));

                        Instant endTime = Instant.now();
                        long duration = Duration.between(startTime, endTime).toSeconds();

                        Logger.log("Calculation took: ".concat(String.valueOf(duration)).concat(" s"));
                        double app_time = duration * ( 2 - result.percentageCalculated );
                        Logger.log("Approximate time for 100%: ".concat(String.valueOf(app_time)));
                    }else{
                        Logger.log("Solution is NOT valid!");
                    }
                    break;
                } else if (answer.getType() == MessageType.CLUSTER_INFO){

                } else {
                    Logger.log("Could not handle message: ".concat(answer.toString()));
                }
            }else{

                this.sendHeartBeat();

                if(clusterConnection.isInterrupted()){
                    this.reconnect();
                    continue;
                }

                Instant start = Instant.now();
                boolean heartBeatReceived = false;
                while(Duration.between(start, Instant.now()).toMillis() <= this.timeout){
                    if(clusterConnection.available()){
                        this.clusterinfo = (ArrayList<WorkerInfo>) clusterConnection.read().getPayload();
                        heartBeatReceived = true;
                    }
                }

                if (!heartBeatReceived){
                    this.reconnect();
                }
            }
        }

        close();
    }

    public void setChiffre(String chiffre){
        this.chiffre= chiffre;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public void close(){
        this.active.set(false);
        clusterConnection.close();
    }
}
