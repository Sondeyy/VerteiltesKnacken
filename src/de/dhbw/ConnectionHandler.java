package de.dhbw;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectionHandler implements Runnable {
    private final Worker worker; // the parent worker of the ConnectionHandler thread
    private final AtomicBoolean active = new AtomicBoolean(true);
    ServerSocket server = null;

    public ConnectionHandler(Worker worker) {
        this.worker = worker;
    }

    public void turnOff() {
        this.active.set(false);

        // close socketserver
        try {
            this.server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
