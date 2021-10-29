package de.dhbw;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.Instant;

/**
 * This class represents a single connection
 */
public class Connection {
    private Socket socket;
    private Instant startTime;
    private Role role;
    private int id;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;

}
