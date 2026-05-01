pragma circom 2.0.0;

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

    // ⚠️ TEMP HASH
    signal computedHash;

    // Compute hash
    computedHash <== bidderSecret * bidderSecret;

    // Constraint check
    computedHash === expectedHash;

    // Set valid = 1 (if constraint passes)
    valid <== 1;

    // 🔥 NULLIFIER (temporary logic)
    nullifier <== bidderSecret + auctionId + bidNonce;
}

component main = BidderVerification();