package de.dhbw.messages;

import java.io.Serializable;


/**
 * This class is used as a container for calculation results of the bruteforce.
 * When the primes were found, use PrimeCalculationResult(p, q)
 * If no primes were found, use PrimeCalculationResult(found=false)
 */
public class PrimeCalculationResult implements Serializable {
    public String p = null;
    public String q = null;
    public double percentageCalculated;
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
