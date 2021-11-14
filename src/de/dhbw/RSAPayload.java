package de.dhbw;

import java.net.InetAddress;

public class RSAPayload {
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
