package de.dhbw;

public class DecryptPayload {
    public final String p;
    public final String q;

    public DecryptPayload(String p, String q) {
        this.p = p;
        this.q = q;
    }

    @Override
    public String toString() {
        return String.format("(%s|%s)",p,q);
    }
}
