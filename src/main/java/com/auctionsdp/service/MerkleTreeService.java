package com.auctionsdp.service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;


public class MerkleTreeService {

    private final int depth = 10;


    public BigInteger buildRoot(List<BigInteger> leaves) {
        if (leaves == null || leaves.isEmpty()) {
            return BigInteger.ZERO;
        }

        List<BigInteger> pathElements = getPathElements(leaves, 0);
        List<Integer> pathIndices = getPathIndices(0);

        return computeRootFromPath(leaves.get(0), pathElements, pathIndices);
    }


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

    public List<Integer> getPathIndices(int index) {
        List<Integer> indices = new ArrayList<>();
        int currentIndex = index;
        for (int d = 0; d < depth; d++) {
            indices.add(currentIndex % 2);
            currentIndex = currentIndex / 2;
        }
        return indices;
    }

   
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