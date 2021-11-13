package de.dhbw;

public enum MessageType {
    OK,             // it's okay to calculate this section
    NOK,            // it's not okay to calculate this section, I'm doing it!
    FREE,           // is this section available?
    RSA,            // start calculation! here is the public key. Payload: [pub]
    START,          // Payload [pub, clientIP, clientPort]
    FINISHED,       // I finished the calculation of my package
    ALTERNATIVES,   // Here you have alternative worker addresses to connect to
    ANSWER_FOUND,    // I did it, here is the solution!!!
    JOIN, // initially join cluster to, get all cluster nodes to connect to
    CONNECT_CLUSTER,
    CLUSTER_INFO,
    HELLO
}
