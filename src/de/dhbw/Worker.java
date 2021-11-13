package de.dhbw;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class Worker implements Runnable {
    private int id;
    private WorkerInfo info;
    private int listenerPort;
    private InetAddress myAddress;
    private int initPort = 0;
    private InetAddress initAddress = null;

    private final ConcurrentHashMap<String, Connection> connections = new ConcurrentHashMap<>(); // handle workers and clients
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
        InetAddress address = connection.getAddress();
        // todo: Really listener Port ?
        int port = connection.getSocket().getPort();
        String key = concatAddressPort(address, port);
        this.connections.put(key,connection);
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
            String key = concatAddressPort(address, port);
            connections.put(key, connection);

            Logger.log(String.format("Connected to: %s:%d", address, port));

            return connection;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void joinCluster(InetAddress address, int port){
        Logger.log("Joining Cluster");
        // connect to arbitrary node in cluster
        Connection initial_connection = connectTo(address, port);
        initial_connection.setRole(Role.WORKER);

        // send JOIN Message to ask for nodes in cluster
        Message join_request = new Message();
        join_request.setType(MessageType.JOIN);
        // include own ListenerPort, so that other workers know how to connect
        join_request.setPayload(new WorkerInfo(listenerPort,myAddress));
        initial_connection.write(join_request);

        Logger.log("SENDING JOIN REQUEST");

        // todo: split up

        Message response = initial_connection.read();
        // cluster_nodes contains all worker nodes except the one connected to
        ConcurrentHashMap<String,Connection> cluster_nodes = (ConcurrentHashMap<String, Connection>) response.getPayload();

        // establish connections with all workers
        for (Connection worker : cluster_nodes.values()) {
            // todo: There is a function "connectTo" that creates a connection and you can call a connect function on a connection
            InetAddress new_address = worker.getAddress();
            int new_port = worker.getlistenerPort();

            // connect to nodes and set them as worker
            Connection new_connection = connectTo(new_address, new_port);
            new_connection.setRole(Role.WORKER);
        }

        // broadcast a request to join the cluster after connecting with every node
        Message connect_cluster_request = new Message();
        connect_cluster_request.setType(MessageType.CONNECT_CLUSTER);
        broadcast(connect_cluster_request);
    }

    public void broadcast(Message message){
        Logger.log("Broadcasting message: ".concat(message.toString()));
        // broadcast message to every connection of node workers


        ArrayList<Role> roles = new ArrayList<>();

        for (Connection connection : connections.values()) {
            roles.add(connection.getRole());
            if(connection.getRole() == Role.WORKER){
                connection.write(message);

                // todo: reminder_ remove

            }
        }

        Logger.log("BROADCAST POSSIBLE ROLES: ".concat(roles.toString()));
    }

    private void reactToMessage(Message message, Connection connection) {
        MessageType messageType = message.getType();
        Role senderRole = connection.getRole();
        Message answer = new Message();

        // the role unknown is only used immediately after connecting, wait for either
        // RSA Message --> connection is client
        // JOIN Message --> connection is worker that wants to join the network

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
            // set the ListenerPort of the connection, that was sent with the JOIN message
            WorkerInfo connection_info = (WorkerInfo) message.getPayload();
            connection.setListenerPort(connection_info.listenerPort);

            // send new worker a Hashmap of all nodes in the network --> without clients
            ConcurrentHashMap<String,Connection> workers_in_cluster = new ConcurrentHashMap<>(connections);
            workers_in_cluster.values().removeIf(entry -> entry.getRole() != Role.WORKER);
            answer.setPayload(workers_in_cluster);
            answer.setType(MessageType.CLUSTER_INFO);
            connection.write(answer);

            // add worker to the cluster
            connection.setRole(Role.WORKER);
            Logger.log(getConnections().toString());

        } else if(messageType == MessageType.CONNECT_CLUSTER) {

            // connection is already established, set role from UNKNOWN to WORKER
            connection.setRole(Role.WORKER);

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

    @Override
    public void run() {
        Logger.log("Running on port: ".concat(Integer.toString(listenerPort)));

        // initialize child threads
        ConnectionHandler connectionHandler = new ConnectionHandler(this);
        Thread connectionHandlerThread = new Thread(connectionHandler);
        connectionHandlerThread.setName(Thread.currentThread().getName().concat(" ConnectionHandler"));
        connectionHandlerThread.start();

        // connect to the cluster if not the first node
        if(!first_node){
            joinCluster(initAddress, initPort);
        }


        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Logger.log("Connections: ".concat(connections.toString()));

        while (active) {
            // get work --> FINISHED_TASK

            // check client and worker messages
            for (Connection connection : connections.values()) {
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

    public ConcurrentHashMap<String, Connection> getConnections() {
        return connections;
    }
}
