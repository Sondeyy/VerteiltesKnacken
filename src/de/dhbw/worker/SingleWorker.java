package de.dhbw.worker;

import de.dhbw.Logger;
import de.dhbw.examhelpers.rsa.RSAHelper;
import de.dhbw.messages.PrimeCalculationResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class SingleWorker {
    public static void main(String[] args) {
        int range = 100;
        String basePath = new File("").getAbsolutePath();
        String file = basePath.concat("/rc/".concat(String.valueOf(range).concat(".txt")));

        ArrayList<String> primes = new ArrayList<>();

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line = br.readLine();

            while (line != null) {
                primes.add(line);
                line = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                Objects.requireNonNull(br).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String publicKey = "298874689697528581074572362022003292763";

        PrimeCalculation primeCalculation = new PrimeCalculation(
                0,
                publicKey,
                primes,
                range);
        Thread thread = new Thread(primeCalculation);
        thread.start();
        PrimeCalculationResult result = primeCalculation.getResult();
        while (result == null) result = primeCalculation.getResult();

        RSAHelper helper = new RSAHelper();
        String chiffre = "b4820013b07bf8513ee59a905039fb631203c8b38ca3d59b475b4e4e092d3979";

        System.out.println(result);
        if(helper.isValid(result.p,result.q,publicKey)) {
            String decryptedText = helper.decrypt(result.p, result.q, chiffre);
            Logger.log("Decrypted Chiffre is: ".concat(decryptedText));
        }
    }
}
