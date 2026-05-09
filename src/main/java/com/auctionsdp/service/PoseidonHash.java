package com.auctionsdp.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.math.BigInteger;


public class PoseidonHash {

    public static BigInteger hash(BigInteger a, BigInteger b) {
        return runNode("buildTreeHash.js", a, b);
    }

   
    public static BigInteger hashSingle(BigInteger a) {
        return runNode("hashSingle.js", a);
    }

    
    private static BigInteger runNode(String script, BigInteger... inputs) {
        try {
            String[] command = new String[inputs.length + 2];
            command[0] = "node";
            command[1] = script;
            for (int i = 0; i < inputs.length; i++) {
                command[i + 2] = inputs[i].toString();
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File("zkp")); // scripts live in zkp/ folder
            pb.redirectErrorStream(true);

            Process process = pb.start();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            String result = reader.readLine();
            System.out.println("Poseidon (" + script + "): " + result);

            if (result == null || !result.matches("\\d+")) {
                throw new RuntimeException(
                    "Invalid Poseidon output from " + script + ": " + result
                );
            }

            return new BigInteger(result);

        } catch (Exception e) {
            throw new RuntimeException("Poseidon hash failed (" + script + ")", e);
        }
    }
}