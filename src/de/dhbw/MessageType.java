package de.dhbw;


/**
 * This enum contains all possible types of a message object
 */
public enum MessageType {
    OK,                 // It's okay to calculate this section
    NOK,                // It's not okay to calculate this section, I'm doing it!
    RSA,                // Sent by Client: Crack my public key! Payload: [pub, clientIP, clientPort(Listener)]
    JOIN,               // I want all nodes in the cluster, so that I can connect to them
    FREE,               // Is this section available?
    START,              // Repacked version of RSA, send RSA Payload but don´t broadcast to nodes in Network
    FINISHED,           // I finished the calculation of my package, also send segment number
    DEAD_NODE,          // I found a dead node: WorkerInfo
    ANSWER_FOUND,       // I did it, here is the solution!!!
    CLUSTER_INFO,       // Here are all members of the cluster for you to connect to (except me)
    CONNECT_CLUSTER,    // Hey, I want to join the cluster, Payload [myAdress, ListenerPort]
}
