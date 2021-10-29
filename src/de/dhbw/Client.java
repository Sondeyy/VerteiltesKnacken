package de.dhbw;

import java.io.IOException;
import java.net.Socket;

public class Client implements Runnable {

    private Socket socket;

    // initialize a connection by starting a socket with dns, port
    public void initializeConnection(int port, String dns) {
        try {
            this.socket = new Socket(dns, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // close the current connection
    public void closeConnection() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // write Message to server, wait for response and return response
    private Message sendMessage(MessageType type, String payload) {
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

        // wait for response
        Message answer = omr.read(socket); // wait for response
        System.out.println("CLIENT RECEIVED: ".concat(answer.toString()));
        return answer;
    }

    @Override
    public void run() {

        // connect to LEADER
        this.initializeConnection(8080, "localhost");

        // send 20 write requests to server
        for (int x = 0; x <= 20; x++) {
            String payload = "Hello World Nr ".concat(String.valueOf(x));
            System.out.println("______________________");
            System.out.println(x);
            this.sendMessage(MessageType.WRITE, payload);
        }

        System.out.println("______________________");

        // send last_ten_messages Message
        this.sendMessage(MessageType.GET_LAST_TEN, null);

        // close connection
        this.closeConnection();

        // connect to FOLLOWER
        this.initializeConnection(8081, "localhost");

        // send 5 write requests to server
        for (int x = 0; x <= 5; x++) {
            System.out.println("______________________");
            String payload = "Hello World Nr ".concat(String.valueOf(x));
            this.sendMessage(MessageType.WRITE, payload);
        }

        System.out.println("______________________");

        // send last_ten_messages Message
        this.sendMessage(MessageType.GET_LAST_TEN, null);
    }
}
