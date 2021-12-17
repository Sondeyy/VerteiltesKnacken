package de.dhbw.worker;

import de.dhbw.*;
import de.dhbw.connection.Connection;
import de.dhbw.messages.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ThreadLocalRandom;
import java.lang.Math;
import java.net.InetAddress;

public class Worker implements Runnable {
    private final int listenerPort;
    private int initPort = 0;
    private InetAddress initAddress = null;
    private final CopyOnWriteArrayList<Connection> connections = new CopyOnWriteArrayList<>(); // handle workers and clients
    private boolean firstNode = false;

    private States state = States.INIT;
    private final AtomicBoolean active = new AtomicBoolean(true);
    private int okCount = 0;

    private int startIndex; // start index of current calculated segment
    private RSAPayload decryptRequestInformation; // Contains public key
    private PrimeCalculation primeCalculation = null; // prime calculation thread

    private final ArrayList<String> primes = new ArrayList<>(); // list of all primes
    private final ArrayList<Integer> segmentStartIndex = new ArrayList<>(); // list of segment start indices
    private final ArrayList<Integer> segmentSizes = new ArrayList<>(); // list of segment sizes for each segmentStartIndex
    // A one at position i in "calculatedSegments" means that the i´th segment was calculated, a zero means it wasn´t.
    // This segment at position i has the start index segmentStartIndex[i] in "primes" and is segmentSizes[i] primes long.
    private ArrayList<Integer> calculatedSegments;

    /**
     * Constructor that is used for the first worker of the cluster, does not connect to any other worker
     * @param ListenerPort open port to listen for new connections
     * @param primeRange range of primes that should be bruteforced
     * @param initialCalculationCount number of calculations per segment
     */
    public Worker(int ListenerPort, int primeRange, int initialCalculationCount) {
        this.listenerPort = ListenerPort;
        this.firstNode = true;

        this.readPrimesFromFile(primeRange);

        this.splitTask(primes.size(), initialCalculationCount);
    }

    /**
     * Constructor that is used for the workers which connect to the cluster via another worker.
     * @param ListenerPort open port to listen for new connections
     * @param primeRange range of primes that should be bruteforced
     * @param initialCalculationCount number of calculations per segment
     * @param initPort Remote worker port to connect to the cluster
     * @param initAddress Remote worker address to connect to the cluster
     */
    public Worker(int ListenerPort, int primeRange, int initialCalculationCount, int initPort, InetAddress initAddress) {
        this.listenerPort = ListenerPort;
        this.initAddress = initAddress;
        this.initPort = initPort;

        this.readPrimesFromFile(primeRange);

        this.splitTask(primes.size(), initialCalculationCount);
    }

    /**
     * Read primes from a file
     * @param range number of primes to read
     */
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

    /**
     * Split the given prime range into subtasks, that all perform at least a certain number of calculations.
     * This accounts for the shrinking number of needed calculations (as discussed in PrimeCalculation) for primes with
     * higher indices when using a fixed segments size by dynamically increasing the segment size for higher primes.
     * Bruteforcing every segment therefore takes approximately the same time, regardless of where the segment is located.
     * @param N Total number of primes
     * @param calculations Number of calculations, that should be exceeded in each segment
     */
    private void splitTask(int N, int calculations) {

        int index = 0;

        while(index <= N-1){
            // add the prime index that starts a given segment
            segmentStartIndex.add(index);
            // calculate the segment size by a given prime index
            int segmentSize = segmentSize(index,N,calculations);
            segmentSizes.add(segmentSize);

            // get index of next segment
            index = index + segmentSize;
        }

        // initialize bitmap for calculated segments
        calculatedSegments = new ArrayList<>(Collections.nCopies(this.segmentSizes.size(), 0));

        // Logging
        Logger.log("Splitting Task:");
        Logger.log("Start indices: ".concat(segmentStartIndex.toString()));
        Logger.log("Segmentsizes: ".concat(segmentSizes.toString()));
        Logger.log("Calculated Segments: ".concat(calculatedSegments.toString()));
    }

    /**
     * Calculate the segment length for a given index of a prime, so that the segments have a total number of
     * calculations between "calculations" and "calculations"+"N". Ideally, "calculations" is a high multiple of N.
     * This accounts for the shrinking number of calculations needed per prime for primes with higher indices,
     * by adjusting the segment size so that the total number of calculations is always slightly greater than the
     * parameter "calculations", regardless of the segment.
     * This problem was reduced to solving a quadratic equation.
     * @param index Start index of the new segment
     * @param N Total number of primes
     * @param calculations minimum number of calculations
     * @return Integer- Minimal segment size that exceeds "calculations"
     */
    private int segmentSize(int index, int N, int calculations) {
        double b = N - index + 0.5;
        double discriminant = b*b - 2*calculations; // the term under the square root of the quadratic formula

        if(discriminant >= 0){
            // When the discriminant is >= 0, the quadratic formula has a solution. This is the segmentSize.
            return (int) Math.ceil(b - Math.sqrt(discriminant));
        }else{
            // The discriminant is smaller than 0, the quadratic formula has no solution. This happens, when the
            // calculatedSegmentsize + N > N. Return a segmentSize, so that index + SegmentSize = N.
            return N - index;
        }
    }

    /**
     * Randomly select a segment which has not been calculated.
     * @return Start index of the random segment
     */
    private int selectPrimeRange() {
        ArrayList<Integer> freeStartIndexes = new ArrayList<>();

        // get start indices of all segments, that have not been calculated before
        int index = 0;
        while (index < segmentSizes.size()) {
            if (calculatedSegments.get(index) == 0) freeStartIndexes.add(index);
            index++;
        }

        // randomly select a start index from the free start indices
        if (freeStartIndexes.size() > 0) {
            int setIndex = ThreadLocalRandom.current().nextInt(0, freeStartIndexes.size());
            return freeStartIndexes.get(setIndex);
        }
        else {
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * Select prime range, broadcast FREE to the cluster to ask if that segment is free.
     */
    private void askForPrimeRange() {
        try{
            this.startIndex = this.selectPrimeRange();
        }catch (IndexOutOfBoundsException e){
            Logger.log("All segments calculated, wait for termination");
            return;
        }

        this.state = States.WAIT_FOR_OK;

        Message request = new Message();
        request.setType(MessageType.FREE);
        request.setPayload(startIndex);

        this.broadcast(request);
    }

    /**
     * Start a new calculation in an extra thread for the selected segments
     */
    private void startCalculation() {
        double percentageCalculated;

        try {
            double sumOfIndexes = calculatedSegments.stream().mapToInt(Integer::intValue).sum();
            percentageCalculated = sumOfIndexes / calculatedSegments.size() * 100;
        } catch (NullPointerException e){
            percentageCalculated = 0;
        }

        Logger.log("--- STARTING CALCULATION - ".concat(String.format("%.1f", percentageCalculated).concat(" %")));
        this.state = States.WORKING;
        
        this.primeCalculation = new PrimeCalculation(
                this.segmentStartIndex.get(startIndex),
                this.decryptRequestInformation.publicKey,
                this.primes,
                this.segmentSizes.get(startIndex)
        );

        // create and start thread
        Thread primeCalculationThread = new Thread(primeCalculation);
        primeCalculationThread.start();
    }

    /**
     * Add a connection to the connections list.
     * @param connection connection to add
     */
    public void appendConnection(Connection connection) {
        connections.add(connection);
    }

    /**
     * Open a socket connection with a worker in the cluster and create a connection Object.
     * @param address Address
     * @param port port
     * @return Connection Object
     */
    public Connection connectTo(InetAddress address, int port) {
        Socket socket;
        try {
            // make a socket connection
            socket = new Socket(address, port);
            // new connection object from socket
            Connection connection = new Connection(socket);
            connection.connectStreamsClient();
            connection.setListenerPort(port);

            Logger.log(String.format("Connected to: %s:%d", address, port));

            return connection;
        } catch (IOException e) {
            Logger.log(String.format("Could not connect to: %s:%d", address, port));
            return null;
        }
    }

    /**
     * Establish a connection to a worker in the cluster and send him a JOIN request.
     * @param address Address of the worker
     * @param port Port of the worker
     * @return connection to the initial worker
     */
    public Connection requestClusterJoin(InetAddress address, int port) {
        //Logging
        Logger.log("JOIN CLUSTER VIA: ".concat(address.toString()).concat(":").concat(Integer.toString(port)));

        // connect to arbitrary node in cluster
        Connection initialConnection = connectTo(address, port);
        if(initialConnection == null) return null;
        initialConnection.setRole(Role.WORKER);

        // send JOIN Message to ask for other nodes in cluster
        Message join_request = new Message();
        join_request.setType(MessageType.JOIN);
        initialConnection.write(join_request);

        return initialConnection;
    }

    /**
     * Connect to all workers of the cluster, specified in "clusterInfo"
     * @param clusterInfo Arraylist<WorkerInfoPayload> Address and Port of all workers in the cluster
     */
    public void connectToCluster(ArrayList<WorkerInfoPayload> clusterInfo) {
        // clusterInfo contains all workers except the one already connected to
        // establish connections with all workers in cluster
        for (WorkerInfoPayload worker : clusterInfo) {
            // connect to worker
            Connection newConnection = connectTo(worker.address, worker.listenerPort);
            // set role
            newConnection.setRole(Role.WORKER);
            // append connection to connections list
            appendConnection(newConnection);
        }

        // broadcast a request to join the cluster after connecting with every node
        Message connectToClusterRequest = new Message();
        connectToClusterRequest.setType(MessageType.CONNECT_CLUSTER);
        // include own ListenerPort, so that other workers know how to connect
        connectToClusterRequest.setPayload(new WorkerInfoPayload(listenerPort, null));
        // broadcast message to all workers
        broadcast(connectToClusterRequest);
    }

    /**
     * Broadcast a message to all workers of the cluster
     * @param message Message to broadcast
     */
    public void broadcast(Message message) {
        Logger.log("SENDING MESSAGE: ".concat(message.toString()));
        // broadcast message to every connection of node workers
        for (Connection connection : connections) {
            if (connection.getRole() == Role.WORKER) {
                connection.write(message);
            }
        }
    }

    /**
     * If the worker is connected to the client, send him the result of the prime calculation
     * @param solution Result of exhaustive key search
     */
    public void PrimeSolutionToClient(PrimeCalculationResult solution) {
        for (Connection connection : connections) {
            if (connection.getRole() == Role.CLIENT) {
                Message solution_message = new Message();
                solution_message.setType(MessageType.ANSWER_FOUND);

                double sumOfIndexes = (double) calculatedSegments.stream().mapToInt(Integer::intValue).sum();
                solution.percentageCalculated = sumOfIndexes / calculatedSegments.size();

                solution_message.setPayload(solution);
                connection.write(solution_message);
            }
        }
    }

    /**
     * React to an incoming message based on it´s type
     * @param message message to which worker should react
     * @param connection connection the message came from
     */
    private void reactToMessage(Message message, Connection connection) {

        Message answer = new Message();

        // handle messages based on their messageType
        switch (message.getType()) {
            case RSA -> {
                /*
                * The RSA message is sent to a worker by a client, it contains the public key and should
                * initialize the calculation. There are two possible scenarios:
                *   1) The client connects the first time, distribute the public key through the cluster with a START
                *       message that initiate the calculation.
                *   2) The client reconnects to this worker, because it´s original worker died. In this case the ongoing
                *      calculation should not start from the beginning, the client is recognized by it´s public key.
                * */

                // unpack the RSA Message and save it
                RSAPayload payload = (RSAPayload) message.getPayload();

                // mark connection as client and set listener port, also in case of a reconnect
                // There the cluster is already calculating the solution for the given public key
                connection.setRole(Role.CLIENT);
                connection.setListenerPort(payload.listenerPort);

                // only distribute the key and initialize the calculation, if public key from the client is not currently
                // processed
                if(this.decryptRequestInformation == null || !this.decryptRequestInformation.publicKey.equals(payload.publicKey)){
                    // this is not client reconnect, start from calculation scratch
                    this.decryptRequestInformation = payload;

                    // broadcast RSA Message payload to all nodes in cluster to init calculation in START message
                    message.setType(MessageType.START);
                    broadcast(message);

                    // begin own calculation
                    this.askForPrimeRange();
                }
            }
            case JOIN, CLUSTER_INFO-> {
                /*
                * A JOIN request is sent by a new worker during it´s connection process. The worker needs
                * information about all workers in the cluster that he needs to connect to. The wroker serving this
                * request with a message of type CLUSTER_INFO.
                *
                * A CLUSTER_INFO request is sent by a client, to get information about all workers in the cluster.
                * If it´s currently connected worker fails, it tries to connect to a different one.
                *
                * In both scenarios, a list of all nodes in the cluster without clients and the current worker is sent.
                * */

                ArrayList<WorkerInfoPayload> clusterInfo = getClusterInfo();
                answer.setPayload(clusterInfo);
                answer.setType(MessageType.CLUSTER_INFO);
                connection.write(answer);
            }
            case CONNECT_CLUSTER -> {
                /*
                * This message is sent by a worker joining the cluster. After it has received port and address
                * information of all nodes in the cluster, it connects to each of them. A CONNECT_CLUSTER message is
                * sent along, it is used to update the information about itself in the remote peer.
                * Mainly the listener port, and the role is changed to WORKER.
                * */

                // set the local ListenerPort of the connection, that was sent with the CONNECT_CLUSTER message
                WorkerInfoPayload connection_info = (WorkerInfoPayload) message.getPayload();
                connection.setListenerPort(connection_info.listenerPort);

                // Set role from UNKNOWN to WORKER
                connection.setRole(Role.WORKER);

            }
            case START -> {
                /*
                * The START message is used to distribute the public key throughout the cluster and to start
                * the calculation.
                * */

                // unpack the RSA Message and save it
                this.decryptRequestInformation = (RSAPayload) message.getPayload();
                // request a segment
                this.askForPrimeRange();
            }
            case FREE -> {
                /*
                 * This message is sent, when asking all nodes in the cluster if a segment is free to calculate.
                 * If the segment is currently calculated, send a NOK, else send a OK.
                 * */
                if (this.state == States.WORKING || this.state == States.WAIT_FOR_OK) {
                    if (this.startIndex == (Integer) message.getPayload()) {
                        Logger.log("SENDING MESSAGE NOK - ".concat(String.valueOf(startIndex)));
                        answer.setType(MessageType.NOK);
                        connection.write(answer);
                        return;
                    }
                }
                answer.setType(MessageType.OK);
                connection.write(answer);
            }
            case OK -> {
                /*
                * After sending a FREE message, the worker receives answers from all nodes in the cluster.
                * If all messages or OK, start calculation.
                * */

                // check if section accepted equals section requested
                if (this.state == States.WAIT_FOR_OK) {
                    this.okCount++;

                    if (this.okCount == this.getWorkerCount()) {
                        Logger.log("All OKS RECEIVED FOR SEGMENT ".concat(Integer.toString(startIndex)));
                        // I am allowed to calculate!
                        this.okCount = 0;
                        this.startCalculation();
                    }
                }
            }
            case NOK -> {
                /*
                 * After sending a FREE message, the worker receives answers from all nodes in the cluster.
                 * If one node sends a NOK reply, request a different random prime range.
                 * */

                if (this.state == States.WAIT_FOR_OK) {
                    this.state = States.FINISHED_TASK;
                    this.okCount = 0;

                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    this.askForPrimeRange();
                }
            }
            case FINISHED -> {
                /*
                * When a worker finished the bruteforce of a segment, he broadcasts a FINISHED message.
                * When a finished message is received, set the entry corresponding to the segment index
                * in "calculatedSegments" to 1.
                * */

                // add solution to solution array
                int startIndexFromMessage = (Integer) message.getPayload();
                this.calculatedSegments.set(startIndexFromMessage, 1);
            }
            case ANSWER_FOUND -> {
                /*
                * When a node finds the solution, he broadcasts a ANSWER_FOUND message. When a node receives this message,
                * it stops it´s calculation and terminates. If it is connected to the client, it sends him the result.
                * */

                // fetch message payload
                PrimeCalculationResult solution = (PrimeCalculationResult) message.getPayload();

                // stop calculation
                this.primeCalculation.stopCalculation();

                // if connected to client, send him ANSWER FOUND message with prime numbers
                PrimeSolutionToClient(solution);

                // shutdown
                this.state = States.FINISHED_TASK;
                this.active.set(false);
            }
            case DEAD_NODE -> {
                /*
                * When a node detects a faulty connection, it broadcasts a DEAD_NODE message with the information about the
                * faulty worker. When received, the other nodes remove this worker from their connection list.
                * */

                WorkerInfoPayload payload = (WorkerInfoPayload) message.getPayload();
                InetAddress address = payload.address;
                int port = payload.listenerPort;

                connections.removeIf(availableConnection -> availableConnection.getAddress() == address && availableConnection.getlistenerPort() == port);
                Logger.log("REMOVED NODE: ".concat(String.valueOf((port))).concat(" ").concat(address.getHostAddress()));
            }
        }
    }

    /**
     * Return the preferred outbound ip, based on: https://stackoverflow.com/a/38342964
     * @return own IP
     */
    private String getOwnIp() {
        try(final DatagramSocket socket = new DatagramSocket()){
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress().getHostAddress();
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get port and address information about all workers in the cluster. If their run on localhost, return outbound ip instead.
     * @return List of all workers in the cluster
     */
    private ArrayList<WorkerInfoPayload> getClusterInfo() {
        try {
            ArrayList<WorkerInfoPayload> cluster_nodes = new ArrayList<>();
            for (Connection connection : connections) {
                if (connection.getRole() == Role.WORKER) {
                    if (connection.getAddress().equals(InetAddress.getByName("127.0.0.1"))) {
                        String own_ip = this.getOwnIp();
                        WorkerInfoPayload worker = new WorkerInfoPayload(connection.getlistenerPort(), InetAddress.getByName(own_ip));
                        cluster_nodes.add(worker);
                    } else {
                        WorkerInfoPayload worker = new WorkerInfoPayload(connection.getlistenerPort(), connection.getAddress());
                        cluster_nodes.add(worker);
                    }
                }
            }
            return cluster_nodes;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Broadcast information to cluster about a node that died.
     * @param connection Connection with the dead node
     */
    private void broadcastDeadNode(Connection connection){
        Message msg = new Message();
        msg.setType(MessageType.DEAD_NODE);
        WorkerInfoPayload deadNodeInfo;
        try {
            if (connection.getAddress().equals(InetAddress.getByName("127.0.0.1"))) {
                String own_ip = this.getOwnIp();
                deadNodeInfo = new WorkerInfoPayload(connection.getlistenerPort(), InetAddress.getByName(own_ip));
            } else {
                deadNodeInfo = new WorkerInfoPayload(connection.getlistenerPort(), connection.getAddress());
            }
            msg.setPayload(deadNodeInfo);
            this.broadcast(msg);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    /**
     * broadcasts found result to all workers in the cluster with an ANSWER_FOUND message
     * @param result result to be broadcasted
     */
    private void broadcastPrimeCalculationResult(PrimeCalculationResult result) {
        Message resultMessage = new Message();
        resultMessage.setType(MessageType.ANSWER_FOUND);
        resultMessage.setPayload(result);
        this.broadcast(resultMessage);
    }

    /**
     * Starting the worker in a thread causes this method to be called.
     * This is the main loop of the worker.
     */
    @Override
    public void run() {
        Logger.log("Listening on port: ".concat(Integer.toString(listenerPort)));

        // initialize child threads
        ConnectionHandler connectionHandler = new ConnectionHandler(this);
        Thread connectionHandlerThread = new Thread(connectionHandler);
        connectionHandlerThread.setName(Thread.currentThread().getName().concat(" ConnectionHandler"));
        connectionHandlerThread.start();

        // connect to the cluster if I am not the first node
        if (!firstNode) {
            // send a JOIN message to the initial node
            Connection initial_connection;
            do {
                initial_connection = requestClusterJoin(initAddress, initPort);

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }while(initial_connection == null);

            appendConnection(initial_connection);

            boolean connectionEstablished = false;
            do {
                if (initial_connection.available()) {

                    Message response = null;
                    try {
                        response = initial_connection.read();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    ArrayList<WorkerInfoPayload> clusterInfo = (ArrayList<WorkerInfoPayload>) response.getPayload();
                    connectToCluster(clusterInfo);
                    connectionEstablished = true;
                } else {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                // wait, until a connection is established
            } while (!connectionEstablished);
        }

        Logger.log("CONNECTED TO: ".concat(connections.toString()));

        while (active.get()) {
            // check if a connection was corrupted, in this case remove it and tell other workers about it
            for (Connection connection : new CopyOnWriteArrayList<>(connections)) {
                if (connection.isInterrupted()) {
                    connections.remove(connection);
                    this.broadcastDeadNode(connection);
                }
            }

            // handle client and worker messages
            for (Connection connection : connections) {
                // if a message is available in the buffer, read and handle it
                if (connection.available()) {
                    Message newMessage = null;
                    try {
                        newMessage = connection.read();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    assert newMessage != null;
                    // handle the accepted message
                    reactToMessage(newMessage, connection);
                }
            }

            // check if PrimeCalculation Thread came to an end
            if (this.state == States.WORKING && this.primeCalculation != null && this.primeCalculation.getResult() != null) {

                // reset the OK counter
                this.okCount = 0;

                // if a solution was found, tell all worker and the client about it and stop execution
                if (this.primeCalculation.getResult().found) {
                    this.broadcastPrimeCalculationResult(this.primeCalculation.getResult());
                    this.PrimeSolutionToClient(this.primeCalculation.getResult());
                    break;
                }
                else {
                    // broadcast a FINISHED message with the segment index
                    Message finished = new Message();
                    finished.setType(MessageType.FINISHED);
                    finished.setPayload(this.startIndex);
                    this.broadcast(finished);

                    // set bitmap
                    calculatedSegments.set(startIndex, 1);

                    this.state = States.FINISHED_TASK;

                    // restart calculation with different segment --> request
                    this.askForPrimeRange();
                }
            }
        }

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
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

    // getters and setters

    public int getListenerPort() {
        return listenerPort;
    }

    public CopyOnWriteArrayList<Connection> getConnections() {
        return connections;
    }

    private int getWorkerCount() {
        int workerCount = 0;
        for (Connection connection : connections) {
            if (connection.getRole() == Role.WORKER) workerCount++;
        }
        return workerCount;
    }

    public void close() {
        this.active.set(false);
    }
}
