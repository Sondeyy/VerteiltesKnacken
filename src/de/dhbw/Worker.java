package de.dhbw;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Worker implements Runnable {
    private int id;
    private Socket socket;
    private final List<Connection> workerConnectionList = new ArrayList<>();
    private final List<Connection> clientConnectionList = new ArrayList<>();
    private Message lastSend;
    private boolean active = true;
    private int okCount = 0;

    public Worker() {
    }

    public void turnOff() {
        this.active = false;
    }

    public void appendConnection(Connection connection) {
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
            // maybe some other worker is down -> wait some time
            // still no NOK after given time -> remove fallen worker from connectionList -> allowed to calc
        } else if (messageType == MessageType.NOK) {
            if (lastSend.getType() == MessageType.FREE) {
                // I'm not allowed
            }
            // doesn't bother me
        } else if (messageType == MessageType.START) {

        } else if (messageType == MessageType.FINISHED) {

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
            if (connection.getId() != this.id) {
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
}
