pragma circom 2.0.0;

include "node_modules/circomlib/circuits/poseidon.circom";
include "node_modules/circomlib/circuits/comparators.circom";
include "./merkleProof.circom";

template BidderVerificationExtended() {

    // PRIVATE INPUTS
    signal input bidderSecret;
    signal input bidAmount;
    signal input pathElements[10];
    signal input pathIndices[10];

    // PUBLIC INPUTS
    signal input auctionId;
    signal input bidNonce;
    signal input expectedHash;
    signal input merkleRoot;
    signal input reservePrice;
    signal input bidCommitment;

    // OUTPUTS
    signal output valid;
    signal output nullifierPoseidon;

    // STEP 1: Verify bidder identity
    component hash = Poseidon(1);
    hash.inputs[0] <== bidderSecret;
    hash.out === expectedHash;

    // STEP 2: Verify merkle membership
    component merkle = MerkleProof(10);
    merkle.leaf <== expectedHash;
    for (var i = 0; i < 10; i++) {
        merkle.pathElements[i] <== pathElements[i];
        merkle.pathIndices[i] <== pathIndices[i];
    }
    merkle.root === merkleRoot;

    // STEP 3: Range proof — bidAmount > reservePrice
    component gt = GreaterThan(64);
    gt.in[0] <== bidAmount;
    gt.in[1] <== reservePrice;
    gt.out === 1;

    // STEP 4: Bid commitment — Poseidon(bidAmount, bidderSecret) === bidCommitment
    component commitmentHash = Poseidon(2);
    commitmentHash.inputs[0] <== bidAmount;
    commitmentHash.inputs[1] <== bidderSecret;
    commitmentHash.out === bidCommitment;

    // STEP 5: Valid output
    valid <== 1;

    // STEP 6: Secure nullifier
    component nullifierHash = Poseidon(3);
    nullifierHash.inputs[0] <== bidderSecret;
    nullifierHash.inputs[1] <== auctionId;
    nullifierHash.inputs[2] <== bidNonce;
    nullifierPoseidon <== nullifierHash.out;
}

component main {public [auctionId, bidNonce, expectedHash, merkleRoot, reservePrice, bidCommitment]} = BidderVerificationExtended();