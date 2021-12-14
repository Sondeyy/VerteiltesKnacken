package de.dhbw;

import de.dhbw.examhelpers.rsa.RSAHelper;

import java.util.ArrayList;

/**
 * This class should be used as an extra Thread for calculating a given range of primes. It bruteforce all combinations
 * of a list of primes and a subset of this list, called a segment. Each combination of two primes is checked, if
 * these primes can construct the private key. It uses an optimization to only calculate half of the needed combinations.
 */
public class PrimeCalculation implements Runnable {
    private final int startIndex;
    private final int segmentSize;
    private final String publicKey;
    private final ArrayList<String> primes;
    private volatile PrimeCalculationResult result = null;

    /**
     * Constructor of Prime calculation.
     * @param startIndex The startIndex of the segment to
     * @param publicKey The public Key
     * @param primes A complete list of all primes. Each prime of a segment is checked against every prime in this list
     * @param segmentSize Number of primes from the startIndex, that should be used for the bruteforce
     */
    public PrimeCalculation(int startIndex, String publicKey, ArrayList<String> primes, int segmentSize) {
        this.startIndex = startIndex;
        this.segmentSize = segmentSize;
        this.publicKey = publicKey;
        this.primes = primes;
    }

    /**
     * Starting the thread causes this method to be called.
     * This method bruteforce all combinations of primes in a
     * segment (primes.subset(startIndex, startIndex + segmentSize)), which is a subset of the whole prime range,
     * with every prime in the list of primes ("primes").
     *
     * Because of the commutativity of multiplication, creating the public modulus (n=p*q) of the two primes p and q
     * yields the same result, if p and q are swapped (p*q == q*p).
     *
     * This means for our exhaustive key search (bruteforce), that when we want to test all combination of the prime p
     * with all primes q_i of the complete prime list, we only need to evaluate the primes q_i, where q_i >= p.
     * All other combinations have already been evaluated before. This is illustrated below:
     *
     *         2  3  5  7 11 13 17 19 23
     *    ------------------------------
     *     2 | 2  3  5  7 11 13 17 19 23
     *     3 |    3  5  7 11 13 17 19 23
     *     5 |       5  7 11 13 17 19 23
     *     7 |          7 11 13 17 19 23
     *    11 |            11 13 17 19 23
     *    13 |               13 17 19 23
     *    17 |                  17 19 23
     *    19 |                     19 23
     *    23 |                        23
     *
     *    The first prime (2) has to be combined with all the primes (2 3 5 7 11 13 17 19 23).
     *    The second prime (3) has to be combined with all primes >= 3 (3 5 7 11 13 17 19 23),
     *    because 2*2 has already evaluated before. This goes on until the last prime (23),
     *    which only needs to be combined with itself.
     */
    @Override
    public void run() {
        RSAHelper helper = new RSAHelper();

        // iterate over all primes p_i of the segment
        for (int i = startIndex; i < startIndex + segmentSize; i++) {
            String p = primes.get(i);

            // for every prime p_i, check all primes q_i >= p_i for a match
            for (int j = startIndex + i; j < primes.size(); j++) {
                String q = primes.get(j);

                if (helper.isValid(p, q, publicKey)) {
                    this.result = new PrimeCalculationResult(p, q);
                    return;
                }
            }
        }
        // right result not found
        this.result = new PrimeCalculationResult(false);
    }

    /**
     * Get the result of the brute force
     * @return PrimeCalculationResult-The result of the bruteforce
     */
    public PrimeCalculationResult getResult() {
        return result;
    }

    /**
     * Stop the calculation
     */
    public void stopCalculation() {
        this.result = new PrimeCalculationResult(false);
    }

}
