package com.auctionsdp.service;

import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ZkpAuctionService {

    private final IdentityRegistry identityRegistry = new IdentityRegistry();
    private final MerkleTreeService merkleTreeService = new MerkleTreeService();
    private final ProofInputBuilder proofInputBuilder = new ProofInputBuilder();

    public Map<String, Object> generateProofInput(
            String userId,
            BigInteger secret,
            BigInteger auctionId,
            BigInteger bidNonce
    ) {

        // 🔥 MUST match circuit Poseidon(1)
        BigInteger expectedHash = PoseidonHash.hashSingle(secret);

        // Register user commitment
        identityRegistry.registerUser(userId, expectedHash);

        // Get all commitments (leaves)
        List<BigInteger> leaves = new ArrayList<>(identityRegistry.getAllCommitments());

        // 🔥 CRITICAL FIX: Merkle tree must have at least 2 leaves
        if (leaves.size() == 1) {
            leaves.add(BigInteger.ZERO);
        }

        // Build Merkle root
        BigInteger merkleRoot = merkleTreeService.buildRoot(leaves);

        // Find index of current user
        int index = leaves.indexOf(expectedHash);

        // 🔒 Safety check
        if (index == -1) {
            throw new RuntimeException("User commitment not found in Merkle tree");
        }

        // Generate Merkle proof
        List<BigInteger> pathElements =
                merkleTreeService.getPathElements(leaves, index);

        List<Integer> pathIndices =
                merkleTreeService.getPathIndices(index);

        // Build final circuit input
        return proofInputBuilder.build(
                secret,
                auctionId,
                bidNonce,
                expectedHash,
                merkleRoot,
                pathElements,
                pathIndices
        );
    }
}