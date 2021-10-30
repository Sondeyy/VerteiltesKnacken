package de.dhbw;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.Instant;

/**
 * This class represents a single connection
 */
public class Connection {
    private final Socket socket;
    private Instant startTime;
    private Role role;
    private int id;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;

    public Connection(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
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
}
