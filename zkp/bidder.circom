pragma circom 2.0.0;

include "node_modules/circomlib/circuits/poseidon.circom";
include "./merkleProof.circom";

template BidderVerification() {

    // 🔐 PRIVATE
    signal input bidderSecret;
    signal input pathElements[10];
    signal input pathIndices[10];

    // 🌍 PUBLIC
    signal input auctionId;
    signal input bidNonce;
    signal input expectedHash;
    signal input merkleRoot;

    // 🔎 OUTPUT
    signal output valid;
    signal output nullifier;

    // 🆕 NEW OUTPUT (secure nullifier)
    signal output nullifierPoseidon;

    // 🔐 Poseidon hash (identity check)
    component hash = Poseidon(1);
    hash.inputs[0] <== bidderSecret;

    // constraint check
    hash.out === expectedHash;

    // ✅ 🆕 MERKLE MEMBERSHIP (ADDED HERE — DO NOT MOVE)
    component merkle = MerkleProof(10);

    merkle.leaf <== expectedHash;

    for (var i = 0; i < 10; i++) {
        merkle.pathElements[i] <== pathElements[i];
        merkle.pathIndices[i] <== pathIndices[i];
    }

    merkle.root === merkleRoot;

    valid <== 1;

    // 🔥 OLD NULLIFIER (kept as-is)
    nullifier <== bidderSecret + auctionId + bidNonce;

    // 🆕 NEW NULLIFIER (secure)
    component nullifierHash = Poseidon(3);
    nullifierHash.inputs[0] <== bidderSecret;
    nullifierHash.inputs[1] <== auctionId;
    nullifierHash.inputs[2] <== bidNonce;

    nullifierPoseidon <== nullifierHash.out;
}

component main = BidderVerification();