package de.dhbw;

public enum MessageType {
    OK,                 // it's okay to calculate this section
    NOK,                // it's not okay to calculate this section, I'm doing it!
    RSA,                // start calculation! here is the public key. Payload: [pub, clientIP, clientPort(Listener)]
    JOIN,               // I want all nodes in the cluster, so that I can connect to them
    FREE,               // Is this section available?
    START,              // Repacked version of RSA, send RSA Payload but don´t broadcast the nodes in Network
    FINISHED,           // I finished the calculation of my package
    ANSWER_FOUND,       // I did it, here is the solution!!!
    CLUSTER_INFO,       // Here are all members of the cluster for you to connect to
    CONNECT_CLUSTER,    // Hey, I want to join the cluster, Payload [myAdress, ListenerPort]
}
