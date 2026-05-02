package com.auctionsdp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Map;

@Service
public class ZkpProofService {

    private static final String ZKP_DIR = "zkp";

    public Map<String, Object> generateProof(Map<String, Object> input) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            // 1. Write input.json
            File inputFile = new File(ZKP_DIR + "/input.json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(inputFile, input);

            // 2. Generate witness
            runCommand("node bidder_js/generate_witness.js bidder_js/bidder.wasm input.json witness.wtns");

            // 3. Generate proof
            runCommand(
                    "snarkjs groth16 prove bidder_final.zkey witness.wtns proof.json public.json"
            );

            // 4. Verify proof
            runCommand(
                    "snarkjs groth16 verify verification_key.json public.json proof.json"
            );

            // 5. Read outputs
            Map proof = mapper.readValue(new File(ZKP_DIR + "/proof.json"), Map.class);
            Object publicSignals = mapper.readValue(new File(ZKP_DIR + "/public.json"), Object.class);

            return Map.of(
                    "proof", proof,
                    "publicSignals", publicSignals
            );

        } catch (Exception e) {
            throw new RuntimeException("ZKP proof generation failed", e);
        }
    }

    private void runCommand(String command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);

        pb.directory(new File(ZKP_DIR));
        pb.redirectErrorStream(true);

        Process process = pb.start();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
        );

        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println("[ZKP] " + line);
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Command failed: " + command);
        }
    }
}