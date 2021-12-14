package de.dhbw.messages;

import java.io.Serializable;
import java.net.InetAddress;

/**
 * This class is used as a payload to send connection information about a worker/workers.
 */
public class WorkerInfoPayload implements Serializable {
    public final int listenerPort;
    public final InetAddress address;

    public WorkerInfoPayload(int listenerPort, InetAddress address) {
        this.listenerPort = listenerPort;
        this.address = address;
    }

    @Override
    public String toString() {
        return String.format("(%d|%s)",listenerPort, address);
    }
}
