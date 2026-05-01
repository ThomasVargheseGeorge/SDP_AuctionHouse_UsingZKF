pragma circom 2.0.0;

include "node_modules/circomlib/circuits/poseidon.circom";

template BidderVerification() {

    // 🔐 PRIVATE
    signal input bidderSecret;

    // 🌍 PUBLIC
    signal input auctionId;
    signal input bidNonce;
    signal input expectedHash;

    // 🔎 OUTPUT
    signal output valid;
    signal output nullifier;

    // 🔐 Poseidon hash
    component hash = Poseidon(1);
    hash.inputs[0] <== bidderSecret;

    // constraint check
    hash.out === expectedHash;

    valid <== 1;

    // 🔥 NULLIFIER (temporary still simple)
    nullifier <== bidderSecret + auctionId + bidNonce;
}

component main = BidderVerification();