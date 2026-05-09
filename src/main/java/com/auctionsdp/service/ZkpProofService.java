package com.auctionsdp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;


@Service
public class ZkpProofService {

    private static final String ZKP_SERVER_URL = "http://localhost:3000/full-prove";

    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> generateProof(Map<String, Object> input) {
        try {
            
            String inputJson = mapper.writeValueAsString(input);

           
            URL url = new URL(ZKP_SERVER_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(60000);

          
            try (OutputStream os = conn.getOutputStream()) {
                byte[] bytes = inputJson.getBytes("utf-8");
                os.write(bytes, 0, bytes.length);
            }

      
            int responseCode = conn.getResponseCode();

            BufferedReader reader;
            if (responseCode == 200) {
                reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "utf-8")
                );
            } else {
                reader = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), "utf-8")
                );
            }

            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
            reader.close();

            String responseJson = responseBuilder.toString();
            System.out.println("[ZKP Server Response] " + responseJson);

            
            Map<String, Object> response = mapper.readValue(responseJson, Map.class);

            
            if (responseCode != 200) {
                throw new RuntimeException(
                    "ZKP server returned error " + responseCode + ": " + responseJson
                );
            }

            Boolean success = (Boolean) response.get("success");
            if (success == null || !success) {
                String error = (String) response.get("error");
                throw new RuntimeException("ZKP proof generation failed: " + error);
            }

            Boolean valid = (Boolean) response.get("valid");
            if (valid == null || !valid) {
                throw new RuntimeException("ZKP proof generated but verification failed");
            }

            
            Map<String, Object> metrics = (Map<String, Object>) response.get("metrics");
            if (metrics != null) {
                System.out.println("[ZKP Metrics] " +
                    "Generation: " + metrics.get("proofGenerationTimeMs") + "ms | " +
                    "Verification: " + metrics.get("verificationTimeMs") + "ms | " +
                    "Size: " + metrics.get("proofSizeBytes") + " bytes"
                );
            }

            return response;

        } catch (Exception e) {
            throw new RuntimeException(
                "ZKP proof generation failed — is Node.js server running at localhost:3000? Error: "
                + e.getMessage(), e
            );
        }
    }

    
    public boolean verifyProof(Map<String, Object> proof, java.util.List<Object> publicSignals) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "proof", proof,
                    "publicSignals", publicSignals
            );

            String inputJson = mapper.writeValueAsString(requestBody);

            URL url = new URL("http://localhost:3000/verify-proof");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(30000);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] bytes = inputJson.getBytes("utf-8");
                os.write(bytes, 0, bytes.length);
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "utf-8")
            );

            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
            reader.close();

            Map<String, Object> response = mapper.readValue(
                    responseBuilder.toString(), Map.class
            );

            Boolean valid = (Boolean) response.get("valid");
            return valid != null && valid;

        } catch (Exception e) {
            System.err.println("Proof verification error: " + e.getMessage());
            return false;
        }
    }
}