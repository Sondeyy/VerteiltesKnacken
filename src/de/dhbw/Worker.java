package de.dhbw;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Worker implements Runnable {
    private int id;
    private int myPort;
    private int initPort;
    private InetAddress initAdress;

    private final ConcurrentHashMap<Integer, Connection> connections = new ConcurrentHashMap<>(); // handle workers and clients
    private final Qeuue broadcasts;
    private boolean active = true;
    private int okCount = 0;
    private States state = States.INIT;

    // state machine ?

    public Worker(int myPort, InetAddress initAddress, int initPort ) {
        this.myPort = myPort;
        this.initAdress = initAddress;
        this.initPort = initPort;
    }

    public void turnOff() {
        this.active = false;
    }

    public synchronized void appendConnection(Connection connection) {
        // KEY ??
        this.connections.put(connection);
    }

    private void send(Message message, Connection connection) {
        connection.write(message);
    }

    private void broadcast(Message message){
        // broadcast to every connection of node workers
    }

    private void reactToMessage(Message message, Connection connection) {
        MessageType messageType = message.getType();
        Message answer = new Message();

        if (messageType == MessageType.OK) {
            // check if section accepted equals section requested
            if (this.state == States.WAIT_FOR_RESOURCE && this.okCount == this.workerConnectionList.size() - 1) {
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

        // connect to

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


    public int getMyPort() {
        return myPort;
    }
}
