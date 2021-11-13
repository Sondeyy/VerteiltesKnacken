package de.dhbw;

import java.io.Serializable;
import java.net.InetAddress;

public class WorkerInfo implements Serializable {
    public final int listenerPort;
    public final InetAddress address;

    public WorkerInfo(int listenerPort, InetAddress address) {
        this.listenerPort = listenerPort;
        this.address = address;
    }

    @Override
    public String toString() {
        return String.format("(%d|%s)",listenerPort, address);
    }
}
