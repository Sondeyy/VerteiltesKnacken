package de.dhbw;

import java.io.Serializable;

public class PrimeCalculationResult implements Serializable {
    public String p = null;
    public String q = null;
    public final boolean found;

    public PrimeCalculationResult(String p, String q) {
        this.p = p;
        this.q = q;
        this.found = true;
    }

    public PrimeCalculationResult(boolean found) {
        this.found = found;
    }

    @Override
    public String toString() {
        return String.format("(%s|%s)", p, q);
    }
}
