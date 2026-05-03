package com.auctionsdp.service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * MerkleTreeService
 *
 * Builds a binary Merkle tree of depth 10 using Poseidon hashing.
 * Supports up to 2^10 = 1024 leaves (registered bidders).
 *
 * FIXES from original:
 * 1. buildRoot now correctly builds the full tree level by level
 *    without padding the root with extra zero hashes afterward
 * 2. getPathElements now traverses the full tree to find the correct
 *    sibling at every level — works for any number of leaves, not just 2
 * 3. All returned paths are exactly `depth` elements long as the circuit expects
 */
public class MerkleTreeService {

    private final int depth = 10;

    // =============================
    // HASH
    // Uses PoseidonHash which delegates to Node.js circomlib
    // Must match exactly what the Circom circuit computes
    // =============================
    public BigInteger hash(BigInteger a, BigInteger b) {
        return PoseidonHash.hash(a, b);
    }

    // =============================
    // BUILD ROOT
    // Pads leaves to the next power of 2 with zeros
    // Then hashes level by level until one root remains
    // =============================
    public BigInteger buildRoot(List<BigInteger> leaves) {
        if (leaves == null || leaves.isEmpty()) {
            return BigInteger.ZERO;
        }

        // Pad to next power of 2 so the tree is always complete
        List<BigInteger> current = padToPowerOfTwo(new ArrayList<>(leaves));

        // Hash level by level until we reach the root
        while (current.size() > 1) {
            List<BigInteger> next = new ArrayList<>();
            for (int i = 0; i < current.size(); i += 2) {
                BigInteger left  = current.get(i);
                BigInteger right = current.get(i + 1); // always exists after padding
                next.add(hash(left, right));
            }
            current = next;
        }

        return current.get(0);
    }

    // =============================
    // GET PATH ELEMENTS
    // Returns the sibling hash at each level from leaf to root
    // These are the pathElements the circuit needs
    // Always returns exactly `depth` elements
    // =============================
    public List<BigInteger> getPathElements(List<BigInteger> leaves, int index) {
        List<BigInteger> path = new ArrayList<>();

        List<BigInteger> current = padToPowerOfTwo(new ArrayList<>(leaves));

        int currentIndex = index;

        for (int level = 0; level < depth; level++) {
            // Sibling is the paired node at this level
            int siblingIndex = (currentIndex % 2 == 0)
                    ? currentIndex + 1
                    : currentIndex - 1;

            // If sibling is within bounds, use it. Otherwise use zero.
            BigInteger sibling = (siblingIndex < current.size())
                    ? current.get(siblingIndex)
                    : BigInteger.ZERO;

            path.add(sibling);

            // Move up one level — hash pairs together
            List<BigInteger> next = new ArrayList<>();
            for (int i = 0; i < current.size(); i += 2) {
                BigInteger left  = current.get(i);
                BigInteger right = (i + 1 < current.size())
                        ? current.get(i + 1)
                        : BigInteger.ZERO;
                next.add(hash(left, right));
            }

            current = next;
            currentIndex = currentIndex / 2;

            // If we have padded beyond actual tree size, fill rest with zeros
            if (current.isEmpty()) {
                while (path.size() < depth) {
                    path.add(BigInteger.ZERO);
                }
                return path;
            }
        }

        // Safety pad — ensure exactly depth elements returned
        while (path.size() < depth) {
            path.add(BigInteger.ZERO);
        }

        return path;
    }

    // =============================
    // GET PATH INDICES
    // Returns the direction at each level: 0 = leaf is left, 1 = leaf is right
    // These are the pathIndices the circuit needs
    // Always returns exactly `depth` elements
    // =============================
    public List<Integer> getPathIndices(int index) {
        List<Integer> indices = new ArrayList<>();
        int currentIndex = index;

        for (int level = 0; level < depth; level++) {
            indices.add(currentIndex % 2); // 0 if left child, 1 if right child
            currentIndex = currentIndex / 2;
        }

        return indices;
    }

    // =============================
    // PRIVATE HELPER
    // Pads a list to the next power of 2 with BigInteger.ZERO
    // Required to make the tree a complete binary tree
    // =============================
    private List<BigInteger> padToPowerOfTwo(List<BigInteger> list) {
        int size = list.size();
        if (size == 0) return list;

        int targetSize = 1;
        while (targetSize < size) {
            targetSize *= 2;
        }

        while (list.size() < targetSize) {
            list.add(BigInteger.ZERO);
        }

        return list;
    }
}