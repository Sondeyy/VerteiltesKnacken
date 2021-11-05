package de.dhbw;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;

public class ConnectionHandler implements Runnable {
    private Worker worker;
    private boolean active = true;

    public ConnectionHandler(Worker worker) {
    }

    public void turnOff() {
        this.active = false;
    }

    @Override
    public void run() {
        ServerSocket server = null;
        try {
            server = new ServerSocket(8080);
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (active) {
            try {
                Socket newSocket = Objects.requireNonNull(server).accept();
                Connection newConnection = new Connection(newSocket);
                worker.appendConnection(newConnection);
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
}
