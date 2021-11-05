package de.dhbw;

public enum MessageType {
    OK,             // it's okay to calculate this section
    NOK,            // it's not okay to calculate this section, I'm doing it!
    FREE,           // is this section available?
    START,          // start calculation! here is the public key
    FINISHED,       // I finished the calculation of my package
    ALTERNATIVES,   // Here you have alternative worker addresses to connect to
    ANSWER_FOUND    // I did it, here is the solution!!!
}
