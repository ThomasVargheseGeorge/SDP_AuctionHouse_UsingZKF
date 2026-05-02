package com.auctionsdp.service;
import java.math.BigInteger;
import java.util.*;

public class IdentityRegistry {

    private final Map<String, BigInteger> users = new HashMap<>();

    public void registerUser(String userId, BigInteger secretHash) {
        users.put(userId, secretHash);
    }

    public List<BigInteger> getAllCommitments() {
        return new ArrayList<>(users.values());
    }

    public BigInteger getUserCommitment(String userId) {
        return users.get(userId);
    }
}
