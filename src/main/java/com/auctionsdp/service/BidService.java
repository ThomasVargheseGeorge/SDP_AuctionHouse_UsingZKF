package com.auctionsdp.service;

import com.auctionsdp.model.Auction;
import com.auctionsdp.model.Bid;
import com.auctionsdp.repository.AuctionRepository;
import com.auctionsdp.repository.BidRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

@Service
public class BidService {

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    // 🔥 Store used nullifiers (prevents proof reuse)
    private final Set<String> usedNullifiers = new HashSet<>();

    // 🔐 SHA-256 Hash (legacy - not used in ZKP flow)
    private String hashBidder(String bidderId) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(bidderId.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing bidderId", e);
        }
    }

    // 🚀 MAIN ZKP METHOD (FINAL VERSION WITH SECURE NULLIFIER)
    public String placeBidWithProof(
            Map<String, Object> proof,
            List<Object> publicSignals,
            Long auctionId,
            Double bidAmount
    ) {

        // ✅ Input validation
        if (proof == null || publicSignals == null || auctionId == null || bidAmount == null) {
            return "Invalid request data";
        }

        // 🔥 MUST be 3 now: [valid, oldNullifier, secureNullifier]
        if (publicSignals.size() < 3) {
            return "Invalid public signals (missing secure nullifier)";
        }

        if (bidAmount <= 0) {
            return "Bid amount must be greater than 0";
        }

        // 🔥 USE SECURE NULLIFIER (Poseidon-based)
        String nullifier = publicSignals.get(2).toString();

        // 🔒 Prevent proof reuse
        if (usedNullifiers.contains(nullifier)) {
            return "❌ Double bidding detected (nullifier already used)";
        }

        // 🔐 Verify ZKP
        boolean isValid = verifyProofExternally(proof, publicSignals);

        if (!isValid) {
            return "❌ Invalid ZKP proof";
        }

        // 🔎 Auction validation
        Optional<Auction> auctionOpt = auctionRepository.findById(auctionId);

        if (auctionOpt.isEmpty()) {
            return "Auction not found";
        }

        Auction auction = auctionOpt.get();

        if (!auction.isActive()) {
            return "Auction is closed";
        }

        if (bidAmount <= auction.getCurrentHighestBid()) {
            return "Bid must be higher than current highest bid";
        }

        // 🔄 Update auction
        auction.setCurrentHighestBid(bidAmount);
        auctionRepository.save(auction);

        // 🧾 Store bid (anonymous)
        Bid bid = new Bid();
        bid.setAuctionId(auctionId);
        bid.setBidAmount(bidAmount);
        bid.setBidderId("ZKP_VERIFIED");

        bidRepository.save(bid);

        // 🔥 Mark nullifier AFTER successful bid
        usedNullifiers.add(nullifier);

        return "✅ Bid placed via ZKP ✔";
    }

    // 🔐 Write JSON to file
    private void writeJsonToFile(Object data, String path) throws Exception {
        mapper.writeValue(new File(path), data);
    }

    // 🔐 ZKP verification using snarkjs
    private boolean verifyProofExternally(Map<String, Object> proof, List<Object> publicSignals) {
        try {

            String zkpPath = "D:/auction/zkp/";

            String tempProofPath = zkpPath + "temp_proof.json";
            String tempPublicPath = zkpPath + "temp_public.json";

            writeJsonToFile(proof, tempProofPath);
            writeJsonToFile(publicSignals, tempPublicPath);

            ProcessBuilder processBuilder = new ProcessBuilder(
                    "cmd", "/c",
                    "snarkjs",
                    "groth16",
                    "verify",
                    zkpPath + "verification_key.json",
                    tempPublicPath,
                    tempProofPath
            );

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream())
            );

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("OK")) {
                    return true;
                }
            }

            process.waitFor();
            return false;

        } catch (Exception e) {
            return false;
        }
    }
}