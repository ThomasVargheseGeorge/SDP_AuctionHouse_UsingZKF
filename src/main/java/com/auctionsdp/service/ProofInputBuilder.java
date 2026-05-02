package com.auctionsdp.service;
import java.math.BigInteger;
import java.util.*;

public class ProofInputBuilder {

    public Map<String, Object> build(
            BigInteger secret,
            BigInteger auctionId,
            BigInteger bidNonce,
            BigInteger expectedHash,
            BigInteger merkleRoot,
            List<BigInteger> pathElements,
            List<Integer> pathIndices
    ) {

        Map<String, Object> input = new HashMap<>();

        input.put("bidderSecret", secret.toString());
        input.put("auctionId", auctionId.toString());
        input.put("bidNonce", bidNonce.toString());
        input.put("expectedHash", expectedHash.toString());
        input.put("merkleRoot", merkleRoot.toString());

        List<String> elements = new ArrayList<>();
        for (BigInteger e : pathElements) {
            elements.add(e.toString());
        }

        List<String> indices = new ArrayList<>();
        for (Integer i : pathIndices) {
            indices.add(i.toString());
        }

        input.put("pathElements", elements);
        input.put("pathIndices", indices);

        return input;
    }
}