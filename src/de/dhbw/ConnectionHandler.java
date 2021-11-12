package de.dhbw;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;

public class ConnectionHandler implements Runnable {
    private final Worker worker;
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
            server = new ServerSocket(getWorker().getId());
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (active) {
            try {
                Socket newSocket = Objects.requireNonNull(server).accept();
                Connection newConnection = new Connection(newSocket);

                // client vs worker connection ?
                worker.appendWorkerConnection(newConnection);
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
