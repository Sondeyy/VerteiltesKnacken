package de.dhbw;

/**
 * This enum represents the states a worker can be in:
 * INIT: The worker did just connect to the cluster and did not start calculating yet
 * WORKING: The worker is calculating a segment
 * FINISHED_TASK: The worker finished calculating a segment, but did not ask for a new task
 * WAIT_FOR_OK: The worker asked if a segment is free and is waiting for responses.
 */
public enum States {
    INIT,
    WORKING,
    FINISHED_TASK,
    WAIT_FOR_OK,
}
