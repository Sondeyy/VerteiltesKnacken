package de.dhbw;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Worker implements Runnable {
    private final int id;
    private int port;
    private InetAddress address;
    private Socket socket;
    private final List<Connection> connectionList = new ArrayList<>(); // handle clients and Workers that have not yet connected
    private final List<Connection> workerConnectionList = new ArrayList<>(); // handle connected Workers
    private Message lastSend;
    private boolean active = true;
    private int okCount = 0;

    // state machine ?

    public Worker(int id, List<Connection> workers) {
        this.id = id;
        for (Connection worker : workers) {
            if(worker.getId() == this.id) {
                this.port = worker.getPort();
                this.address = worker.getAdress();
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
            if(connection.getAdress() == newConnection.getAdress() && connection.getPort() == newConnection.getPort()){
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
        this.lastSend = message;
    }

    private void reactToMessage(Message message, Connection connection) {
        MessageType messageType = message.getType();
        Message answer = new Message();

        if (messageType == MessageType.OK) {
            // check if section accepted equals section requested
            if (lastSend.getType() == MessageType.FREE && this.okCount == this.workerConnectionList.size() - 1) {
                // I am allowed to calc
            }
            // else
            // maybe some other worker is down -> wait some random time
            // still no NOK after given time -> remove fallen worker from connectionList -> allowed to calc
        } else if (messageType == MessageType.NOK) {
            if (lastSend.getType() == MessageType.FREE) {
                // I'm not allowed
            }
            // doesn't bother me
        } else if (messageType == MessageType.START) {

        } else if (messageType == MessageType.FINISHED) {
            // add solution to solution array

        } else if (messageType == MessageType.ANSWER_FOUND) {

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
