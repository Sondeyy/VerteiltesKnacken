package de.dhbw;

import java.io.Serializable;
import java.net.InetAddress;


/**
 * This class is used for the client to send the public Key to the cluster and initiate a calculation.
 */
public class RSAPayload implements Serializable {
    public final int listenerPort;
    public final InetAddress address;
    public final String publicKey;

    public RSAPayload(String publicKey, int listenerPort, InetAddress address) {
        this.publicKey = publicKey;
        this.listenerPort = listenerPort;
        this.address = address;
    }

    @Override
    public String toString() {
        return String.format("(%s|%d|%s)",publicKey, listenerPort, address);
    }
}
