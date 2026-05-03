pragma circom 2.0.0;

include "node_modules/circomlib/circuits/poseidon.circom";
include "./merkleProof.circom";

template BidderVerification() {

    // =============================
    // PRIVATE INPUTS
    // =============================
    signal input bidderSecret;
    signal input pathElements[10];
    signal input pathIndices[10];

    // =============================
    // PUBLIC INPUTS
    // =============================
    signal input auctionId;
    signal input bidNonce;
    signal input expectedHash;
    signal input merkleRoot;

    // =============================
    // OUTPUTS
    // =============================
    signal output valid;
    signal output nullifierPoseidon;

    // STEP 1: Verify bidder identity
    // Prove bidderSecret hashes to expectedHash
    component hash = Poseidon(1);
    hash.inputs[0] <== bidderSecret;
    hash.out === expectedHash;

    // STEP 2: Verify merkle membership
    // Prove this bidder is in the registered bidders tree
    component merkle = MerkleProof(10);
    merkle.leaf <== expectedHash;
    for (var i = 0; i < 10; i++) {
        merkle.pathElements[i] <== pathElements[i];
        merkle.pathIndices[i] <== pathIndices[i];
    }
    merkle.root === merkleRoot;

    // STEP 3: Mark proof as valid
    valid <== 1;

    // STEP 4: Secure nullifier
    // Poseidon(bidderSecret, auctionId, bidNonce)
    // Insecure addition nullifier removed
    component nullifierHash = Poseidon(3);
    nullifierHash.inputs[0] <== bidderSecret;
    nullifierHash.inputs[1] <== auctionId;
    nullifierHash.inputs[2] <== bidNonce;
    nullifierPoseidon <== nullifierHash.out;
}

component main {public [auctionId, bidNonce, expectedHash, merkleRoot]} = BidderVerification();