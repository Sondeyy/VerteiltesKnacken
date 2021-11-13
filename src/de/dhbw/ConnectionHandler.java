package de.dhbw;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;

public class ConnectionHandler implements Runnable {
    private final Worker worker; // the parent worker of the ConnectionHandler thread
    private boolean active = true;

    public ConnectionHandler(Worker worker) {
        this.worker = worker;
    }

    public void turnOff() {
        this.active = false;
    }

    @Override
    public void run() {
        ServerSocket server = null;
        try {
            Logger.log("Opening Serversocket on port: ".concat(Integer.toString(worker.getListenerPort())));
            server = new ServerSocket(worker.getListenerPort(), 100, worker.getMyAddress());
        } catch (IOException e) {
            e.printStackTrace();
            Logger.log("Failed to connect Serversocket on Port: ".concat(Integer.toString(worker.getListenerPort())));
        }
        while (active) {
            try {
                Socket newSocket = Objects.requireNonNull(server).accept();
                Connection newConnection = new Connection(newSocket);

                // client vs worker connection ?
                newConnection.connectStreamsServer();
                worker.appendConnection(newConnection);
                Logger.log("Connection Established!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            Objects.requireNonNull(server).close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Worker getWorker() {
        return worker;
    }
}
