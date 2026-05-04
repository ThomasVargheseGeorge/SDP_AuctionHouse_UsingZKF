package com.auctionsdp.service;

import java.math.BigInteger;
import java.util.*;

/**
 * ProofInputBuilder
 *
 * Stage 2 update:
 * - Added bidAmount, reservePrice, bidCommitment to circuit input
 * - Old build() method kept for Stage 1 baseline circuit (bidder.circom)
 * - New buildExtended() method for Stage 2 circuit (bidder_extended.circom)
 */
public class ProofInputBuilder {

    // =============================
    // STAGE 1 — original circuit input
    // Used for baseline benchmarking comparison
    // =============================
    public Map<String, Object> build(
            BigInteger secret,
            BigInteger auctionId,
            BigInteger bidNonce,
            BigInteger expectedHash,
            BigInteger merkleRoot,
            List<BigInteger> pathElements,
            List<Integer> pathIndices
    ) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("bidderSecret", secret.toString());
        input.put("auctionId", auctionId.toString());
        input.put("bidNonce", bidNonce.toString());
        input.put("expectedHash", expectedHash.toString());
        input.put("merkleRoot", merkleRoot.toString());
        input.put("pathElements", toStringList(pathElements));
        input.put("pathIndices", toIndexStringList(pathIndices));
        return input;
    }

    // =============================
    // STAGE 2 — extended circuit input
    // Includes bidAmount, reservePrice, bidCommitment
    // This is the combined circuit — the novelty claim
    // =============================
    public Map<String, Object> buildExtended(
            BigInteger secret,
            BigInteger auctionId,
            BigInteger bidNonce,
            BigInteger expectedHash,
            BigInteger merkleRoot,
            List<BigInteger> pathElements,
            List<Integer> pathIndices,
            BigInteger bidAmount,
            BigInteger reservePrice,
            BigInteger bidCommitment
    ) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("bidderSecret", secret.toString());
        input.put("bidAmount", bidAmount.toString());         // private — hidden
        input.put("auctionId", auctionId.toString());
        input.put("bidNonce", bidNonce.toString());
        input.put("expectedHash", expectedHash.toString());
        input.put("merkleRoot", merkleRoot.toString());
        input.put("reservePrice", reservePrice.toString());  // public — minimum bid
        input.put("bidCommitment", bidCommitment.toString()); // public — Poseidon(bidAmount, secret)
        input.put("pathElements", toStringList(pathElements));
        input.put("pathIndices", toIndexStringList(pathIndices));
        return input;
    }

    // =============================
    // HELPERS
    // =============================
    private List<String> toStringList(List<BigInteger> list) {
        List<String> result = new ArrayList<>();
        for (BigInteger e : list) result.add(e.toString());
        return result;
    }

    private List<String> toIndexStringList(List<Integer> list) {
        List<String> result = new ArrayList<>();
        for (Integer i : list) result.add(i.toString());
        return result;
    }
}