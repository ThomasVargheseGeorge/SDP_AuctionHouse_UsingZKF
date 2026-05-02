package com.auctionsdp.service;
import com.auctionsdp.service.PoseidonHash;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class MerkleTreeService {

    private final int depth = 10;

    public BigInteger hash(BigInteger a, BigInteger b) {
        return PoseidonHash.hash(a, b);
    }

    public BigInteger buildRoot(List<BigInteger> leaves) {

        if (leaves == null || leaves.isEmpty()) {
            return BigInteger.ZERO;
        }

        List<BigInteger> current = new ArrayList<>(leaves);

        while (current.size() > 1) {
            List<BigInteger> next = new ArrayList<>();

            for (int i = 0; i < current.size(); i += 2) {
                BigInteger left = current.get(i);
                BigInteger right = (i + 1 < current.size()) ? current.get(i + 1) : left;

                next.add(hash(left, right));
            }

            current = next;
        }

        BigInteger root = current.get(0);

        // pad to depth
        for (int i = 1; i < depth; i++) {
            root = hash(root, BigInteger.ZERO);
        }

        return root;
    }

    // ✅ FIXED: always return EXACTLY depth elements
    public List<BigInteger> getPathElements(List<BigInteger> leaves, int index) {
        List<BigInteger> path = new ArrayList<>();

        if (leaves == null || leaves.size() < 2) {
            // 🔥 Critical fix: pad with zeros
            for (int i = 0; i < depth; i++) {
                path.add(BigInteger.ZERO);
            }
            return path;
        }

        // sibling
        path.add(leaves.get(1 - index));

        // pad remaining
        while (path.size() < depth) {
            path.add(BigInteger.ZERO);
        }

        return path;
    }

    // ✅ always return EXACTLY depth indices
    public List<Integer> getPathIndices(int index) {
        List<Integer> indices = new ArrayList<>();

        indices.add(index);

        while (indices.size() < depth) {
            indices.add(0);
        }

        return indices;
    }
}