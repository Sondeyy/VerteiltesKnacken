package de.dhbw;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class Worker implements Runnable {
    private final int listenerPort;
    private final InetAddress myAddress;
    private int initPort = 0;
    private InetAddress initAddress = null;

    // todo: maybe change back to ConcurrentHashmap
    private final CopyOnWriteArrayList<Connection> connections = new CopyOnWriteArrayList<>(); // handle workers and clients
    // private final Queue broadcasts;
    // todo: own thread for broadcasts ?
    private RSAPayload decryptRequestInformation;
    private final AtomicBoolean active = new AtomicBoolean(true);
    private int okCount = 0;
    private States state = States.INIT;
    private final ArrayList<String> primes = new ArrayList<>();
    private boolean first_node = false;
    private int startIndex;
    private int segmentSize;
    private PrimeCalculation primeCalculation;

    // state machine ?

    public Worker(int ListenerPort, InetAddress myAddress, int primeRange, int segmentSize) {
        this.listenerPort = ListenerPort;
        this.myAddress = myAddress;
        this.first_node = true;
        this.segmentSize = segmentSize;

        this.readPrimesFromFile(primeRange);

    }

    public Worker(int ListenerPort, InetAddress myAddress, int initPort, InetAddress initAddress, int primeRange, int segmentSize) {
        this.listenerPort = ListenerPort;
        this.myAddress = myAddress;
        this.initAddress = initAddress;
        this.initPort = initPort;
        this.segmentSize = segmentSize;

        this.readPrimesFromFile(primeRange);
    }

    private void readPrimesFromFile(int range) {
        String basePath = new File("").getAbsolutePath();
        String file = basePath.concat("/rc/".concat(String.valueOf(range).concat(".txt")));

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line = br.readLine();

            while (line != null) {
                this.primes.add(line);
                line = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                Objects.requireNonNull(br).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int selectPrimeRange() {
        int primesAvailable = primes.size();

        ArrayList<Integer> workerPorts = new ArrayList<>();

        // TODO need to differ based on ports, when running on one device, but on hostname, when on multiple
        for (Connection connection : connections) {
            if (connection.getRole() == Role.WORKER) {
                workerPorts.add(connection.getlistenerPort());
            }
        }

        int workersAvailable = workerPorts.size();

        // TODO test and check if this makes sense at all -> prototype version

        workerPorts.add(this.listenerPort);
        workerPorts.sort(Comparator.naturalOrder());
        int myPlace = workerPorts.indexOf(this.listenerPort);

        int startIndex = myPlace * (primesAvailable / workersAvailable);

        return startIndex;
    }

    private void askForPrimeRange() {
        this.startIndex = this.selectPrimeRange();
        this.state = States.WAIT_FOR_OK;

        Message request = new Message();

        request.setType(MessageType.FREE);
        request.setPayload(startIndex);

        this.broadcast(request);
    }

    private void startCalculation() {
        this.primeCalculation = new PrimeCalculation(
                this.startIndex,
                this.decryptRequestInformation.publicKey,
                this.primes,
                this.segmentSize
        );

        Thread primeCalculationThread = new Thread(primeCalculation);
        primeCalculationThread.start();
    }

    public void appendConnection(Connection connection) {
        connections.add(connection);
    }

    public Connection connectTo(InetAddress address, int port) {
        Socket socket;
        try {
            // make a new socket on "myPort"
            socket = new Socket(address, port);

            Connection connection = new Connection(socket);
            connection.connectStreamsClient();
            connection.setListenerPort(port);
            connections.add(connection);

            Logger.log(String.format("Connected to: %s:%d", address, port));

            return connection;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Connection requestClusterJoin(InetAddress address, int port) {
        Logger.log("JOIN CLUSTER VIA: ".concat(address.toString()).concat(":").concat(Integer.toString(port)));

        // connect to arbitrary node in cluster
        Connection initial_connection = connectTo(address, port);
        initial_connection.setRole(Role.WORKER);

        // send JOIN Message to ask for other nodes in cluster
        Message join_request = new Message();
        join_request.setType(MessageType.JOIN);

        initial_connection.write(join_request);

        return initial_connection;
    }

    public void joinCluster(ArrayList<WorkerInfo> clusterInfo) {
        // clusterInfo contains all workers except the one connected to
        // establish connections with all workers in cluster
        for (WorkerInfo worker : clusterInfo) {

            Connection new_connection = connectTo(worker.address, worker.listenerPort);
            new_connection.setRole(Role.WORKER);

            // todo: Call Append here ? --> Give Listener Port as parameter, when accepting connections, set Listener Port correctly ?
        }

        // broadcast a request to join the cluster after connecting with every node
        Message connect_cluster_request = new Message();
        connect_cluster_request.setType(MessageType.CONNECT_CLUSTER);
        // include own ListenerPort, so that other workers know how to connect
        connect_cluster_request.setPayload(new WorkerInfo(listenerPort, myAddress));
        broadcast(connect_cluster_request);
    }

    public void broadcast(Message message) {
        // todo: Outsource to own Thread ?
        Logger.log("Broadcasting message: ".concat(message.toString()));
        // broadcast message to every connection of node workers

        for (Connection connection : connections) {
            if (connection.getRole() == Role.WORKER) {
                connection.write(message);
            }
        }
    }

    public void ifConnectedToClientSendAnswer(PrimeCalculationResult solution) {
        for (Connection connection : connections) {
            if (connection.getRole() == Role.CLIENT) {
                Message solution_message = new Message();
                solution_message.setType(MessageType.ANSWER_FOUND);
                solution_message.setPayload(solution);
                connection.write(solution_message);
            }
        }
    }

    private void reactToMessage(Message message, Connection connection) {

        Message answer = new Message();

        // handle messages based on their messageType
        switch (message.getType()) {
            case RSA -> {
                // unpack the RSA Message and save it
                RSAPayload payload = (RSAPayload) message.getPayload();
                this.decryptRequestInformation = payload;

                // mark connection as client and set Listener port
                connection.setRole(Role.CLIENT);
                connection.setListenerPort(payload.listenerPort);

                // broadcast RSA Message to all nodes in cluster to init calculation
                message.setType(MessageType.START);
                broadcast(message);

                // todo: start own calculation

            }
            case JOIN -> {
                // send new worker a List of all nodes in the cluster --> without clients

                ArrayList<WorkerInfo> clusterInfo = getClusterInfo();
                answer.setPayload(clusterInfo);
                answer.setType(MessageType.CLUSTER_INFO);
                connection.write(answer);

            }
            case CONNECT_CLUSTER -> {
                // set the local ListenerPort of the connection, that was sent with the CONNECT_CLUSTER message
                WorkerInfo connection_info = (WorkerInfo) message.getPayload();
                connection.setListenerPort(connection_info.listenerPort);

                // Set role from UNKNOWN to WORKER
                connection.setRole(Role.WORKER);

            }
            case CLUSTER_INFO -> {

                // obsolete ? --> just handle in main run loop

            }
            case START -> {
                // this is RSA message, that is distributed throughout the cluster to supply all nodes with the public
                // key and to start the calculation

                // unpack the RSA Message and save it
                this.decryptRequestInformation = (RSAPayload) message.getPayload();

                // todo: start calculation

            }
            case OK -> {
                // check if section accepted equals section requested
                if (this.state == States.WAIT_FOR_OK) {
                    // I am allowed to calc
                }
                // else
                // maybe some other worker is down -> wait some random time
                // still no NOK after given time -> remove fallen worker from connectionList -> allowed to calc
            }
            case NOK -> {
                if (!(this.state == States.WAIT_FOR_OK)) {
                    // I am not allowed to calc
                }
                // doesn't bother me
            }
            case FINISHED -> {
                // add solution to solution array

            }
            case ANSWER_FOUND -> {
                // fetch message payload
                PrimeCalculationResult solution = (PrimeCalculationResult) message.getPayload();

                // stop calculation

                // if connected to client, send him ANSWER FOUND message with prime numbers
                ifConnectedToClientSendAnswer(solution);
            }
            case FREE -> {

            }
        }
    }

    private ArrayList<WorkerInfo> getClusterInfo() {
        ArrayList<WorkerInfo> cluster_nodes = new ArrayList<>();

        for (Connection connection : connections) {
            if (connection.getRole() == Role.WORKER) {
                WorkerInfo worker = new WorkerInfo(connection.getlistenerPort(), connection.getAddress());
                cluster_nodes.add(worker);
            }
        }

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
        if (!first_node) {
            Connection initial_connection = requestClusterJoin(initAddress, initPort);

            boolean connectionEstablished = false;
            do {
                if (initial_connection.available()) {

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

        while (active.get()) {
            // get work --> FINISHED_TASK

            // handle client and worker messages
            for (Connection connection : connections) {
                if (connection.available()) {
                    Message newMessage = connection.read();
                    Logger.log("Data available: ".concat(newMessage.toString()));
                    reactToMessage(newMessage, connection);
                }
            }

            // check if PrimeCalculation came to an end
            if (this.primeCalculation.getResult() != null) {
                if (this.primeCalculation.getResult().found) {
                    this.sendResult(this.primeCalculation.getResult());
                }
                else {
                    this.state = States.FINISHED_TASK;
                    this.askForPrimeRange();
                }
            }
        }

        // close connectionHandler Thread
        connectionHandler.turnOff();
        try {
            connectionHandlerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // close sockets of every connection
        for (Connection connection : connections) {
            connection.close();
        }

        Logger.log("SUCCESSFULLY CLOSED");
    }

    private void sendResult(PrimeCalculationResult result) {
        // TODO
    }

    // getters and setters

    public int getListenerPort() {
        return listenerPort;
    }

    public InetAddress getMyAddress() {
        return myAddress;
    }

    public CopyOnWriteArrayList<Connection> getConnections() {
        return connections;
    }

    public void close() {
        this.active.set(false);
    }
}
