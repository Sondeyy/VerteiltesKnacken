package de.dhbw;

import java.io.IOException;
import java.net.Socket;

public class Client implements Runnable {
    private Socket socket;

    /**
     * @param port port to connect to
     * @param dns  host to connect to
     */
    public void initializeConnection(int port, String dns) {
        try {
            this.socket = new Socket(dns, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * close the connection
     */
    public void closeConnection() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param type    message type
     * @param payload payload
     */
    private void sendMessage(MessageType type, String payload) {
        ObjectMessageReader omr = new ObjectMessageReader();

        // create WRITE message
        Message msg = new Message();
        msg.setPayload(payload);
        msg.setSender("Client");
        msg.setReceiver("Server");
        msg.setType(type);

        // send payload to socket
        omr.send(socket, msg);
        System.out.println("CLIENT SEND: ".concat(msg.toString()));
    }

    @Override
    public void run() {

        // connect to LEADER
        this.initializeConnection(19999, "localhost");

        for( int i = 0; i < 10; i++)
            Logger.log("Sending ping");
            this.sendMessage(MessageType.JOIN, "ping");

        this.closeConnection();
    }
}
