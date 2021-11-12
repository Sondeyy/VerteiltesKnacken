package de.dhbw;

import java.io.*;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Worker implements Runnable {
    private final int id;
    private int port;
    private InetAddress address;
    private Socket socket;
    private State state;
    private final List<Connection> clientConnectionList = new ArrayList<>(); // handle clients and Workers that have not yet connected
    private final List<Connection> workerConnectionList = new ArrayList<>(); // handle connected Workers
    private List<Connection> okFrom = new ArrayList<>();
    private boolean active = true;
    private Instant askedOk;
    private final List<BigInteger> primes = new ArrayList<>();

    // state machine ?

    public Worker(int id, List<Connection> workers, int range) {
        this.id = id;
        for (Connection worker : workers) {
            if(worker.getId() == this.id) {
                this.port = worker.getPort();
                this.address = worker.getAddress();
            }
        }

        String basePath = new File("").getAbsolutePath();
        String file = basePath.concat("/rc/".concat(String.valueOf(range).concat(".txt")));

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line = br.readLine();

            while (line != null) {
                primes.add(new BigInteger(line));
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

    public void turnOff() {
        this.active = false;
    }

    public void appendWorkerConnection(Connection newConnection) {

        Connection connection;

        // if worker is already in list, replace worker
        for (int i = 0; i < workerConnectionList.size(); i++) {
            connection = workerConnectionList.get(i);
            if(connection.getAddress() == newConnection.getAddress() && connection.getPort() == newConnection.getPort()){
                workerConnectionList.set(i,newConnection);
                return;
            }
        }

        this.clientConnectionList.add(newConnection);
    }

    public void appendClientConnection(Connection connection) {
        this.workerConnectionList.add(connection);
    }

    private void send(Message message, Connection connection) {
        connection.write(message);
    }

    private void reactToMessage(Message message, Connection connection) {
        MessageType messageType = message.getType();
        Message answer = new Message();

        // if msg is not for me and no broadcast -> don't react to it
        if (!(message.getReceiver() == this.id || message.getReceiver() == 0)) {
            return;
        }

        if (messageType == MessageType.OK) {
            // check if section accepted equals section requested
            if (state == State.WAITING) {
                okFrom.add(connection);

                if (okFrom.size() == this.workerConnectionList.size()) {
                    // I am allowed to calc
                }
            }
        } else if (messageType == MessageType.NOK) {
            if (state == State.WAITING) {
                // I'm not allowed
                okFrom = new ArrayList<>();
            }
            // doesn't bother me
        } else if (messageType == MessageType.START) {
            List<Connection> connectionsReceived;
            connectionsReceived = (List<Connection>) message.getPayload();

            for (Connection connectionReceived : connectionsReceived) {
                this.appendWorkerConnection(connectionReceived);
            }
        } else if (messageType == MessageType.FINISHED) {
            // add solution to solution array


        } else if (messageType == MessageType.ANSWER_FOUND) {
            // somebody found the solution -> finish
        } else if (messageType == MessageType.FREE) {
        }

        send(answer, connection);
    }

    @Override
    public void run() {
        ConnectionHandler connectionHandler = new ConnectionHandler(this);
        Thread connectionHandlerThread = new Thread(connectionHandler);
        connectionHandlerThread.start();

        for (Connection connection : workerConnectionList) {
            if (connection.getId() != this.id && connection.getSocket() == null) {
                try {
                    connection.connect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        while (active) {
            for (Connection connection : workerConnectionList) {
                if (connection.getId() != this.id) {
                    if (connection.available() != 0) {
                        Message newMessage = connection.read();
                        reactToMessage(newMessage, connection);
                    }
                }
            }
            if (state == State.WAITING && Duration.between(askedOk, Instant.now()).toSeconds() > 1) {
                // not enough workers answered my request
                // some worker is down

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
    public int getId() {
        return id;
    }
}
