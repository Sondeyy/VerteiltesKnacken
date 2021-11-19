package de.dhbw;

import de.dhbw.examhelpers.rsa.PrimeRange;
import de.dhbw.examhelpers.rsa.RSAHelper;

import java.math.BigInteger;
import java.util.ArrayList;

/**
 * This class should be used as an extra Thread for calculating a given range of primes
 */
public class PrimeCalculation implements Runnable {
    private final int startIndex;
    private final String publicKey;
    private final ArrayList<String> primes;

    public PrimeCalculation(int startIndex, String publicKey, ArrayList<String> primes) {
        this.startIndex = startIndex;
        this.publicKey = publicKey;
        this.primes = primes;
    }

    @Override
    public void run() {
        RSAHelper helper = new RSAHelper();


    }
}
