package com.auctionsdp.service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * MerkleTreeService - Fixed to match circuit exactly.
 *
 * The circuit computes root by:
 * - Starting from leaf
 * - At each level i:
 *   if pathIndices[i] == 0: Poseidon(current, pathElement[i])
 *   if pathIndices[i] == 1: Poseidon(pathElement[i], current)
 *
 * Java must compute root THE SAME WAY.
 * buildRoot now uses computeRootFromPath internally
 * so both are guaranteed to match.
 */
public class MerkleTreeService {

    private final int depth = 10;

    // =============================
    // BUILD ROOT
    // Gets path for leaf at index 0, then computes root
    // using the exact same logic as the circuit
    // =============================
    public BigInteger buildRoot(List<BigInteger> leaves) {
        if (leaves == null || leaves.isEmpty()) {
            return BigInteger.ZERO;
        }

        List<BigInteger> pathElements = getPathElements(leaves, 0);
        List<Integer> pathIndices = getPathIndices(0);

        return computeRootFromPath(leaves.get(0), pathElements, pathIndices);
    }

    // =============================
    // COMPUTE ROOT FROM PATH
    // Mirrors the circuit's merkleProof.circom EXACTLY
    // This is the single source of truth for root computation
    // =============================
    public BigInteger computeRootFromPath(
            BigInteger leaf,
            List<BigInteger> pathElements,
            List<Integer> pathIndices
    ) {
        BigInteger current = leaf;

        for (int i = 0; i < depth; i++) {
            BigInteger sibling = pathElements.get(i);
            int index = pathIndices.get(i);

            if (index == 0) {
                current = PoseidonHash.hash(current, sibling);
            } else {
                current = PoseidonHash.hash(sibling, current);
            }
        }

        return current;
    }

    // =============================
    // GET PATH ELEMENTS
    // Returns sibling hash at each level for given leaf index
    // Always returns exactly depth elements
    // =============================
    public List<BigInteger> getPathElements(List<BigInteger> leaves, int index) {
        List<BigInteger> path = new ArrayList<>();
        List<BigInteger> level = padToPowerOfTwo(new ArrayList<>(leaves));
        int currentIndex = index;

        for (int d = 0; d < depth; d++) {
            int siblingIndex = (currentIndex % 2 == 0)
                    ? currentIndex + 1
                    : currentIndex - 1;

            BigInteger sibling = (siblingIndex < level.size())
                    ? level.get(siblingIndex)
                    : BigInteger.ZERO;

            path.add(sibling);

            List<BigInteger> nextLevel = new ArrayList<>();
            for (int i = 0; i + 1 < level.size(); i += 2) {
                nextLevel.add(PoseidonHash.hash(level.get(i), level.get(i + 1)));
            }
            if (nextLevel.isEmpty()) {
                nextLevel.add(PoseidonHash.hash(level.get(0), BigInteger.ZERO));
            }

            level = nextLevel;
            currentIndex = currentIndex / 2;
        }

        while (path.size() < depth) {
            path.add(BigInteger.ZERO);
        }

        return path;
    }

    // =============================
    // GET PATH INDICES
    // Returns direction at each level: 0 = left, 1 = right
    // =============================
    public List<Integer> getPathIndices(int index) {
        List<Integer> indices = new ArrayList<>();
        int currentIndex = index;
        for (int d = 0; d < depth; d++) {
            indices.add(currentIndex % 2);
            currentIndex = currentIndex / 2;
        }
        return indices;
    }

    // =============================
    // BUILD ROOT FOR SPECIFIC LEAF
    // Used when we need the root from any leaf's perspective
    // =============================
    public BigInteger buildRootForLeaf(List<BigInteger> leaves, int index) {
        List<BigInteger> pathElements = getPathElements(leaves, index);
        List<Integer> pathIndices = getPathIndices(index);
        return computeRootFromPath(leaves.get(index), pathElements, pathIndices);
    }

    private List<BigInteger> padToPowerOfTwo(List<BigInteger> list) {
        int targetSize = 1;
        while (targetSize < list.size()) targetSize *= 2;
        while (list.size() < targetSize) list.add(BigInteger.ZERO);
        return list;
    }
}