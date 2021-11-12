package de.dhbw;

import java.io.Serializable;
import java.time.Instant;

/**
 * This class is a sendable message object
 */

public class Message implements Serializable {
    private int sender;
    private int receiver;
    private Object payload;
    private Instant time = Instant.now();
    private MessageType type;
    private int sequenceNo = -1;

    /* GETTER - SETTER */

    public int getSender() {
        return sender;
    }

    public void setSender(int sender) {
        this.sender = sender;
    }

    public int getReceiver() {
        return receiver;
    }

    public void setReceiver(int receiver) {
        this.receiver = receiver;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public Instant getTime() {
        return time;
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public int getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(int sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    @Override
    public String toString() {
        String payload_string = getPayload() != null ? getPayload().toString() : "no Payload";
        return String.valueOf(this.getSender()).concat(": ").concat(this.getTime().toString())
                .concat(" - ").concat(this.getType().toString())
                .concat(" - ").concat(payload_string);
    }
}
