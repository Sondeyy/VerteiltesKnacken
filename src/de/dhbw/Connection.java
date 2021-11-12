package de.dhbw;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

/**
 * This class represents a single connection
 */
public class Connection {
    transient private Socket socket = null;
    private Role role;
    private int id;
    private int port;
    private InetAddress address;
    transient private ObjectOutputStream objectOutputStream;
    transient private ObjectInputStream objectInputStream;

    public Connection(Socket socket) {
        this.socket = socket;

        if (this.socket != null) {
            this.port = socket.getPort();
            this.address = socket.getInetAddress();
        }
    }

    public void connect() throws IOException {
        // only connect, if socket is not already connected
        if (this.socket != null) {
            this.socket = new Socket(address, port);

            // create input and output streams
            InputStream is = this.socket.getInputStream();
            this.objectInputStream = new ObjectInputStream(is);
            OutputStream os = this.socket.getOutputStream();
            this.objectOutputStream = new ObjectOutputStream(os);
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPort() {
        return port;
    }

    public InetAddress getAddress() {
        return address;
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

    public void write(Message message) {
        try {
            this.objectOutputStream.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
