package de.dhbw;

public enum MessageType {
    OK,                 // it's okay to calculate this section
    NOK,                // it's not okay to calculate this section, I'm doing it!
    RSA,                // start calculation! here is the public key. Payload: [pub]
    JOIN,               // I want all nodes in the cluster, so that I can connect to them
    FREE,               // Is this section available?
    HELLO,              // Simple test message
    START,              // Payload [pub, clientIP, clientPort(Listener)]
    FINISHED,           // I finished the calculation of my package
    ALTERNATIVES,       // Here you have alternative worker addresses to connect to (if i ) --> obsolete, workers should connect
    ANSWER_FOUND,       // I did it, here is the solution!!!
    CLUSTER_INFO,       // Here are all members of the cluster for you to connect to
    CONNECT_CLUSTER,    // Hey, I want to join the cluster, Payload [myAdress, ListenerPort]
}
