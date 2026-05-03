package com.auctionsdp.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.math.BigInteger;

/**
 * PoseidonHash
 *
 * Delegates Poseidon hashing to Node.js circomlib scripts.
 * This ensures Java Merkle tree construction produces identical hashes
 * to what the Circom circuit computes — critical for proof validity.
 *
 * Two scripts in the zkp/ folder:
 *   buildTreeHash.js  — Poseidon(a, b) used for Merkle tree nodes
 *   hashSingle.js     — Poseidon(a)    used for bidder identity commitment
 *
 * If these scripts are missing or return wrong output,
 * ALL proofs will fail silently. Always test these independently first.
 */
public class PoseidonHash {

    // =============================
    // MERKLE NODE HASH — Poseidon(a, b)
    // Used when building the Merkle tree in MerkleTreeService
    // =============================
    public static BigInteger hash(BigInteger a, BigInteger b) {
        return runNode("buildTreeHash.js", a, b);
    }

    // =============================
    // IDENTITY HASH — Poseidon(a)
    // Used to compute a bidder's expectedHash from their secret
    // This is what gets stored as the Merkle leaf
    // =============================
    public static BigInteger hashSingle(BigInteger a) {
        return runNode("hashSingle.js", a);
    }

    // =============================
    // INTERNAL: Run a Node.js script with BigInteger arguments
    // Scripts must print exactly one line: the numeric result
    // =============================
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