package de.dhbw;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Worker implements Runnable {
    private int id;
    private WorkerInfo info;
    // todo: consolidate
    private int listenerPort;
    private InetAddress myAddress;
    private int initPort = 0;
    private InetAddress initAddress = null;

    private final CopyOnWriteArrayList<Connection> connections = new CopyOnWriteArrayList<>(); // handle workers and clients
    // private final Qeuue broadcasts;
    private HashMap<String,Object> decryptRequestInformation;
    private boolean active = true;
    private int okCount = 0;
    private States state = States.INIT;
    private boolean first_node = false;

    // state machine ?

    public Worker(int ListenerPort, InetAddress myAdress){
        this.listenerPort = ListenerPort;
        this.myAddress = myAdress;
        this.first_node = true;
    }

    public Worker(int ListenerPort, InetAddress myAddress, int initPort , InetAddress initAddress) {
        this.listenerPort = ListenerPort;
        this.myAddress = myAddress;
        this.initAddress = initAddress;
        this.initPort = initPort;
    }

    public void turnOff() {
        this.active = false;
    }

    public void appendConnection(Connection connection) {
        // InetAddress address = connection.getAddress();
        // int port = connection.getSocket().getPort();
        //String key = concatAddressPort(address, port);
        //this.connections.put(key,connection);

        connections.add(connection);
    }

    private String concatAddressPort(InetAddress address, int port){
        return address.toString().concat(":").concat(Integer.toString(port));
    }

    public Connection connectTo(InetAddress address, int port){
        Socket socket = null;
        try {
            // make a new socket on "myPort"
            socket = new Socket(address, port);

            Connection connection = new Connection(socket);
            connection.connectStreamsClient();
            connections.add(connection);
            // todo: Rausmachen, und ID verwenden ?

            Logger.log(String.format("Connected to: %s:%d", address, port));

            return connection;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Connection requestClusterJoin(InetAddress address, int port){
        Logger.log("JOIN CLUSTER VIA: ".concat(address.toString()).concat(":").concat(Integer.toString(port)));

        // connect to arbitrary node in cluster
        Connection initial_connection = connectTo(address, port);
        initial_connection.setRole(Role.WORKER);

        // send JOIN Message to ask for nodes in cluster
        Message join_request = new Message();
        join_request.setType(MessageType.JOIN);

        initial_connection.write(join_request);

        return initial_connection;
    }

    public void joinCluster(ArrayList<WorkerInfo> clusterInfo){
        // clusterInfo contains all workers except the one connected to
        // establish connections with all workers in cluster
        for (WorkerInfo worker : clusterInfo) {

            Connection connection = null;
            // connect to nodes and set them as worker
            for (Connection test_connection : connections) {
                if(test_connection.getlistenerPort())
            }
            // todo: Problem. Not everyone knows Listener Port of whole cluster  

            Connection new_connection = connectTo(worker.address, worker.listenerPort);
            new_connection.setRole(Role.WORKER);
            new_connection.setListenerPort(worker.listenerPort);

            // todo: Call Append here ? --> Give Listener Port as parameter, when accepting connections, set Listener Port correctly ?
        }

        // broadcast a request to join the cluster after connecting with every node
        Message connect_cluster_request = new Message();
        connect_cluster_request.setType(MessageType.CONNECT_CLUSTER);
        // include own ListenerPort, so that other workers know how to connect
        connect_cluster_request.setPayload(new WorkerInfo(listenerPort, myAddress));
        broadcast(connect_cluster_request);
    }

    public void broadcast(Message message){
        // todo: Outsource to own Thread ?
        Logger.log("Broadcasting message: ".concat(message.toString()));
        // broadcast message to every connection of node workers

        for (Connection connection : connections) {
            if(connection.getRole() == Role.WORKER){
                connection.write(message);
            }
        }
    }

    private void reactToMessage(Message message, Connection connection) {
        MessageType messageType = message.getType();
        Message answer = new Message();

        if(messageType == MessageType.RSA){

            // mark connection as client
            connection.setRole(Role.CLIENT);

            // broadcast START Message to nodes in cluster to init calculation
            answer.setType(MessageType.START);

            // append the client ip and port to the payload
            HashMap<String,Object> payload = (HashMap<String, Object>) message.getPayload();
            payload.put("client_ip", connection.getAddress());
            // todo: ListenerPort ?
            payload.put("client_port", connection.getlistenerPort());
            answer.setPayload(payload);

            // save hashmap with public key and client information
            decryptRequestInformation = payload;

            broadcast(answer);

        } else if(messageType == MessageType.JOIN){

            // send new worker a List of all nodes in the cluster --> without clients
            ArrayList<WorkerInfo> clusterInfo = getClusterInfo();
            answer.setPayload(clusterInfo);
            answer.setType(MessageType.CLUSTER_INFO);
            connection.write(answer);

        } else if(messageType == MessageType.CONNECT_CLUSTER) {
            // set the local ListenerPort of the connection, that was sent with the CONNECT_CLUSTER message
            WorkerInfo connection_info = (WorkerInfo) message.getPayload();
            connection.setListenerPort(connection_info.listenerPort);

            // connection is already established, set role from UNKNOWN to WORKER
            connection.setRole(Role.WORKER);

        } else if(messageType == MessageType.CLUSTER_INFO){

            //

        } else if (messageType == MessageType.OK) {
            // check if section accepted equals section requested
            if (this.state == States.WAIT_FOR_RESOURCE) {
                // I am allowed to calc
            }
            // else
            // maybe some other worker is down -> wait some random time
            // still no NOK after given time -> remove fallen worker from connectionList -> allowed to calc
        } else if (messageType == MessageType.NOK) {
            if (this.state == States.WAIT_FOR_RESOURCE) {
                // I am not allowed to calc
            }
            // doesn't bother me
        } else if (messageType == MessageType.FINISHED) {
            // add solution to solution array

        } else if (messageType == MessageType.ANSWER_FOUND) {

        } else if (messageType == MessageType.FREE) {

        }
    }

    private ArrayList<WorkerInfo> getClusterInfo(){
        ArrayList<WorkerInfo> cluster_nodes = new ArrayList<>();

        for (Connection connection : connections) {
            if(connection.getRole() == Role.WORKER){
                WorkerInfo worker = new WorkerInfo(connection.getlistenerPort(), connection.getAddress());
                cluster_nodes.add(worker);
            }
        }

        // add information about myself
        cluster_nodes.add(new WorkerInfo(listenerPort, myAddress));

        return cluster_nodes;
    }

    @Override
    public void run() {
        Logger.log("Listening on port: ".concat(Integer.toString(listenerPort)));

        // initialize child threads
        ConnectionHandler connectionHandler = new ConnectionHandler(this);
        Thread connectionHandlerThread = new Thread(connectionHandler);
        connectionHandlerThread.setName(Thread.currentThread().getName().concat(" ConnectionHandler"));
        connectionHandlerThread.start();

        // connect to the cluster if I am not the first node
        if(!first_node){
            Connection initial_connection = requestClusterJoin(initAddress, initPort);

            boolean connectionEstablished = false;
            do {
                if(initial_connection.available() != 0){

                    Message response = initial_connection.read();
                    ArrayList<WorkerInfo> clusterInfo = (ArrayList<WorkerInfo>) response.getPayload();
                    joinCluster(clusterInfo);
                    connectionEstablished = true;
                } else {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } while (!connectionEstablished);
        }

        Logger.log("Connections: ".concat(connections.toString()));

        while (active) {
            // get work --> FINISHED_TASK

            // check client and worker messages
            for (Connection connection : connections) {
                if (connection.available() != 0) {
                    Message newMessage = connection.read();
                    Logger.log("Data available: ".concat(newMessage.toString()));
                    reactToMessage(newMessage, connection);
                }
            }
        }

        connectionHandler.turnOff();
        try {
            connectionHandlerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // getters and setters


    public int getListenerPort() {
        return listenerPort;
    }

    public InetAddress getMyAddress(){
        return myAddress;
    }

    public CopyOnWriteArrayList<Connection> getConnections() {
        return connections;
    }
}
