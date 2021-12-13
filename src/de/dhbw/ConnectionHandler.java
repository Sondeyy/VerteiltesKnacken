package de.dhbw;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class implements a Serversocket, that can accept incoming socket connections.
 * Every Worker class has a "ConnectionHandler" running in a Thread, that accepts incoming socket connections,
 * connects the input and outputstreams and creates a "Connection" object that is added
 * to the "connections" list of a worker.
 */
public class ConnectionHandler implements Runnable {
    private final Worker worker; // the parent worker of the ConnectionHandler thread
    private final AtomicBoolean active = new AtomicBoolean(true);
    ServerSocket server = null;

    /**
     * Constructor for a connectionHandler. Pass a reference to the parent worker.
     * @param worker The parent worker
     */
    public ConnectionHandler(Worker worker) {
        this.worker = worker;
    }

    /**
     * Turns the connectionHandler off, server socket is no longer listening.
     */
    public void turnOff() {
        this.active.set(false);

        // close socketserver
        try {
            this.server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Start a thread for the connectionHandler, until interrupted, accept all incoming connections
     * to the connectionHandler, and add the connection to the worker.
     */
    @Override
    public void run() {
        try {
            Logger.log("Opening Serversocket on port: ".concat(Integer.toString(worker.getListenerPort())));
            server = new ServerSocket(worker.getListenerPort(), 500);
        } catch (IOException e) {
            e.printStackTrace();
            Logger.log("Failed to connect Serversocket on Port: ".concat(Integer.toString(worker.getListenerPort())));
        }
        while (active.get()) {
            try {
                Socket newSocket = Objects.requireNonNull(server).accept();
                Connection newConnection = new Connection(newSocket);

                // client vs worker connection ?
                newConnection.connectStreamsServer();
                worker.appendConnection(newConnection);
                Logger.log("Connection Established!");
            } catch (IOException e) {
                // everything fine
            }
        }
        try {
            Objects.requireNonNull(server).close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
