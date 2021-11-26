package de.dhbw;

import de.dhbw.examhelpers.rsa.RSAHelper;

import java.util.ArrayList;

/**
 * This class should be used as an extra Thread for calculating a given range of primes
 */
public class PrimeCalculation implements Runnable {
    private final int startIndex;
    private final int segmentSize;
    private final String publicKey;
    private final ArrayList<String> primes;
    private volatile PrimeCalculationResult result = null;

    public PrimeCalculation(int startIndex, String publicKey, ArrayList<String> primes, int segmentSize) {
        this.startIndex = startIndex;
        this.segmentSize = segmentSize;
        this.publicKey = publicKey;
        this.primes = primes;
    }

    @Override
    public void run() {
        RSAHelper helper = new RSAHelper();

        String firstPrime = primes.get(startIndex);
        for (int i = startIndex; i < startIndex + segmentSize; i++) {
            String p = primes.get(i);

            for (int j = startIndex + i; j < primes.size(); j++) {
                String q = primes.get(j);

                if (helper.isValid(p, q, publicKey)) {
                    this.result = new PrimeCalculationResult(p, q);
                    Logger.log("FOUND RESULT!!!!!!!!!!!!!!!!!!!!!!!!!!#########");
                    return;
                }
            }
        }
        // right result not found
        this.result = new PrimeCalculationResult(false);
    }

    public PrimeCalculationResult getResult() {
        return result;
    }

    public void stopCalculation() {
        this.result = new PrimeCalculationResult(false);
    }

}
