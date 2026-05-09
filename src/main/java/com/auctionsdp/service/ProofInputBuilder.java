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
        input.put("bidAmount", bidAmount.toString());        
        input.put("auctionId", auctionId.toString());
        input.put("bidNonce", bidNonce.toString());
        input.put("expectedHash", expectedHash.toString());
        input.put("merkleRoot", merkleRoot.toString());
        input.put("reservePrice", reservePrice.toString());  
        input.put("bidCommitment", bidCommitment.toString()); 
        input.put("pathElements", toStringList(pathElements));
        input.put("pathIndices", toIndexStringList(pathIndices));
        return input;
    }

    
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