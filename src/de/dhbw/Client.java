package de.dhbw;

import de.dhbw.examhelpers.rsa.RSAHelper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * This Class implements a client, that can connect to the cluster and send a request to crack a public key.
 * If the node of the cluster fails, that is connected to the client, the client will try to reconnect to a different
 * node of the cluster. It will then resend it´s request to crack the public key, and the cluster will recognize the
 * previous session.
 */
public class Client implements Runnable {
    private final InetAddress address;
    private final int clusterPort;
    private final InetAddress clusterAddress;
    private Connection clusterConnection;
    int timeout = 1000;

    private ArrayList<WorkerInfo> clusterinfo;

    private String chiffre;
    private String publicKey;

    private final AtomicBoolean active = new AtomicBoolean(true);

    /**
     * Constructor of Client.
     * @param address Own IP address
     * @param clusterPort Port of the worker to connect
     * @param clusterAddress Address of the worker to connect
     */
    public Client(InetAddress address, int clusterPort, InetAddress clusterAddress) {
        this.address = address;
        this.clusterPort = clusterPort;
        this.clusterAddress = clusterAddress;
    }

    /**
     * This method connects the client to the cluster by creating a socket and Input/Output Streams.
     * @param port port to connect to
     * @param address  host to connect to
     */
    public void connectTo(int port, InetAddress address) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(address, port), timeout);
            // create connection object
            Connection connection = new Connection(socket);
            connection.connectStreamsClient();
            connection.setListenerPort(port);
            this.clusterConnection = connection;

            Logger.log(String.format("Connected to: %s:%d", address, port));
        } catch (IOException e) {
            Logger.log(String.format("Could not connect to: %s:%d", address, port));
        }
    }

    /**
     * Send a Message with type RSA to the cluster, which contains the public key to crack.
     * This initializes the calculation.
     * @param publicKey The public Key
     */
    private void sendPublicKey(String publicKey) {
        if(clusterConnection != null){
            // create RSA message
            Message msg = new Message();
            // todo: Does only need to contain publicKey ?
            RSAPayload payload = new RSAPayload(publicKey, 0, address);
            msg.setPayload(payload);
            msg.setType(MessageType.RSA);

            // send payload to cluster
            this.clusterConnection.write(msg);
        } else {
            Logger.log("Connect to cluster before sending RSA Key!");
        }
    }


    /**
     * Send a heartbeat signal to the connected node of the cluster. This sends a message of type CLUSTER_INFO,
     * to get information about the current nodes in the cluster.
     */
    private void sendHeartBeat(){
        Message heartBeat = new Message();
        heartBeat.setType(MessageType.CLUSTER_INFO);

        // todo: Handle Fail
        this.clusterConnection.write(heartBeat);
    }


    /**
     * Reconnect to the cluster if the connected node fails. Subsequently connect to one of the nodes in the cluster
     * and send him the public key, until a working connection is established.
     */
    private void reconnect(){
        // reconnect to one of the nodes in the cluster
        for (WorkerInfo clusterNode : clusterinfo) {
            Logger.log(String.format("Reconnection to: %s: %d", clusterNode.address, clusterNode.listenerPort));
            connectTo(clusterNode.listenerPort, clusterNode.address);

            // resend the publicKey to the cluster
            sendPublicKey(publicKey);
            // todo: Maybe waiut for answer ?
            // if sending the public key failed, try connecting to the next node in the cluster, else break
            if(!clusterConnection.isInterrupted()){
                break;
            }
            Logger.log(String.format("Could not reconnect to: %s: %d", clusterNode.address, clusterNode.listenerPort));
        }
            Logger.log("Successful reconnect!");
    }

    /**
     * Starts the client in it´s own thread.
     */
    @Override
    public void run() {
        // connect to cluster
        connectTo(clusterPort, clusterAddress);

        // send publicKey to cluster
        sendPublicKey(publicKey);

        // start timer
        Instant startTime = Instant.now();

        // create decryption helper
        RSAHelper helper = new RSAHelper();

        // wait for cluster responses
        while(active.get()){
            if(clusterConnection.available()){

                Message answer = null;
                try {
                    answer = clusterConnection.read();
                } catch (IOException e) {
                    this.reconnect();
                    continue;
                }

                // the cluster found an answer
                if (answer.getType() == MessageType.ANSWER_FOUND) {

                    PrimeCalculationResult result = ((PrimeCalculationResult) answer.getPayload());

                    Logger.log("Received solution: ".concat(answer.toString()));
                    Logger.log("Percentage of segments: ".concat(String.valueOf(result.percentageCalculated)));
                    PrimeCalculationResult solution = (PrimeCalculationResult) answer.getPayload();

                    // decrypt chiffre and approximate time for 100% calculation
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
                    this.clusterinfo = (ArrayList<WorkerInfo>) answer.getPayload();
                } else {
                    Logger.log("Could not handle message: ".concat(answer.toString()));
                }
            }else{
                // send a heartbeat signal to the server, to check, if it is still available
                this.sendHeartBeat();

                // reconnect to a different node, if heartbeat did not go through.
                if(clusterConnection.isInterrupted()){
                    this.reconnect();
                }

                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        close();
    }

    // getters and setters

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
