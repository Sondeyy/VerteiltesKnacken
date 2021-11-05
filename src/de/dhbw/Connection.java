package de.dhbw;

import java.io.*;
import java.net.Socket;
import java.time.Instant;

/**
 * This class represents a single connection
 */
public class Connection {
    transient private Socket socket;
    // private Role role;
    private int id;
    private int port;
    private String dns;
    transient private ObjectOutputStream objectOutputStream;
    transient private ObjectInputStream objectInputStream;

    public Connection(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }

    public void connect() throws IOException {
        this.socket = new Socket(dns, port);

        // create input and output streams
        InputStream is = this.socket.getInputStream();
        this.objectInputStream = new ObjectInputStream(is);
        OutputStream os = this.socket.getOutputStream();
        this.objectOutputStream = new ObjectOutputStream(os);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int available() {
        try {
            return objectInputStream.available();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public Message read() {
        try {
            return (Message) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void write(Message message){
        try {
            this.objectOutputStream.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
