package com.auctionsdp.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.io.File;

public class PoseidonHash {

    // 🔥 MERKLE HASH (2 inputs)
    public static BigInteger hash(BigInteger a, BigInteger b) {
        return runNode("buildTreeHash.js", a, b);
    }

    // 🔥 SINGLE INPUT HASH (for expectedHash)
    public static BigInteger hashSingle(BigInteger a) {
        return runNode("hashSingle.js", a);
    }

    // 🔧 COMMON EXECUTION METHOD
    private static BigInteger runNode(String script, BigInteger... inputs) {
        try {
            String[] command = new String[inputs.length + 2];
            command[0] = "node";
            command[1] = script;

            for (int i = 0; i < inputs.length; i++) {
                command[i + 2] = inputs[i].toString();
            }

            ProcessBuilder pb = new ProcessBuilder(command);

            // ✅ run inside zkp folder
            pb.directory(new File("zkp"));
            pb.redirectErrorStream(true);

            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            String result = reader.readLine();

            System.out.println("Node output (" + script + "): " + result);

            if (result == null || !result.matches("\\d+")) {
                throw new RuntimeException("Invalid Poseidon output: " + result);
            }

            return new BigInteger(result);

        } catch (Exception e) {
            throw new RuntimeException("Poseidon hash failed (" + script + ")", e);
        }
    }
}