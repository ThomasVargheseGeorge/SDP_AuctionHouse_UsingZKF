package com.auctionsdp.service;

import org.springframework.stereotype.Service;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ZkpAuctionService
 *
 * Stage 2 update:
 * - generateProofInput() kept for Stage 1 baseline circuit
 * - generateExtendedProofInput() added for Stage 2 combined circuit
 *   Accepts bidAmount and reservePrice
 *   Computes bidCommitment = Poseidon(bidAmount, bidderSecret) in Java
 *   Passes all values to the extended circuit
 */
@Service
public class ZkpAuctionService {

    private final IdentityRegistry identityRegistry = new IdentityRegistry();
    private final MerkleTreeService merkleTreeService = new MerkleTreeService();
    private final ProofInputBuilder proofInputBuilder = new ProofInputBuilder();

    // =============================
    // STAGE 1 — original proof input
    // Kept for baseline benchmarking
    // =============================
    public Map<String, Object> generateProofInput(
            String userId,
            BigInteger secret,
            BigInteger auctionId,
            BigInteger bidNonce
    ) {
        BigInteger expectedHash = PoseidonHash.hashSingle(secret);
        identityRegistry.registerUser(userId, expectedHash);

        List<BigInteger> leaves = new ArrayList<>(identityRegistry.getAllCommitments());
        if (leaves.size() == 1) leaves.add(BigInteger.ZERO);

        int index = leaves.indexOf(expectedHash);
        if (index == -1) throw new RuntimeException("User commitment not found in Merkle tree");

        List<BigInteger> pathElements = merkleTreeService.getPathElements(leaves, index);
        List<Integer> pathIndices = merkleTreeService.getPathIndices(index);
        BigInteger merkleRoot = merkleTreeService.computeRootFromPath(expectedHash, pathElements, pathIndices);

        System.out.println("[ZKP Stage1] expectedHash: " + expectedHash);
        System.out.println("[ZKP Stage1] merkleRoot:   " + merkleRoot);

        return proofInputBuilder.build(
                secret, auctionId, bidNonce,
                expectedHash, merkleRoot,
                pathElements, pathIndices
        );
    }

    // =============================
    // STAGE 2 — extended proof input
    // Includes bid amount (hidden), reserve price, bid commitment
    // bidCommitment = Poseidon(bidAmount, bidderSecret)
    // This is computed in Java and passed as a PUBLIC input to the circuit
    // The circuit verifies the commitment matches internally
    // =============================
    public Map<String, Object> generateExtendedProofInput(
            String userId,
            BigInteger secret,
            BigInteger auctionId,
            BigInteger bidNonce,
            BigInteger bidAmount,
            BigInteger reservePrice
    ) {
        // Step 1: Identity hash
        BigInteger expectedHash = PoseidonHash.hashSingle(secret);
        identityRegistry.registerUser(userId, expectedHash);

        // Step 2: Merkle tree
        List<BigInteger> leaves = new ArrayList<>(identityRegistry.getAllCommitments());
        if (leaves.size() == 1) leaves.add(BigInteger.ZERO);

        int index = leaves.indexOf(expectedHash);
        if (index == -1) throw new RuntimeException("User commitment not found in Merkle tree");

        List<BigInteger> pathElements = merkleTreeService.getPathElements(leaves, index);
        List<Integer> pathIndices = merkleTreeService.getPathIndices(index);
        BigInteger merkleRoot = merkleTreeService.computeRootFromPath(expectedHash, pathElements, pathIndices);

        // Step 3: Compute bid commitment
        // Poseidon(bidAmount, bidderSecret)
        // This is stored on the server — never the plain amount
        BigInteger bidCommitment = PoseidonHash.hash(bidAmount, secret);

        System.out.println("[ZKP Stage2] expectedHash:   " + expectedHash);
        System.out.println("[ZKP Stage2] merkleRoot:     " + merkleRoot);
        System.out.println("[ZKP Stage2] bidCommitment:  " + bidCommitment);
        System.out.println("[ZKP Stage2] reservePrice:   " + reservePrice);

        return proofInputBuilder.buildExtended(
                secret, auctionId, bidNonce,
                expectedHash, merkleRoot,
                pathElements, pathIndices,
                bidAmount, reservePrice, bidCommitment
        );
    }
}