package com.auctionsdp.service;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class NullifierStore {

    private final Set<String> usedNullifiers = new HashSet<>();

    public boolean isUsed(String nullifier) {
        return usedNullifiers.contains(nullifier);
    }

    public void markUsed(String nullifier) {
        usedNullifiers.add(nullifier);
    }
}