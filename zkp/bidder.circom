pragma circom 2.0.0;

template BidderVerification() {

    // Private input
    signal input bidderSecret;

    // Public input
    signal input expectedHash;

    // Output
    signal output valid;

    signal computedHash;
    signal diff;

    // Simulated hash
    computedHash <== bidderSecret * bidderSecret;

    // Enforce equality: diff = computedHash - expectedHash
    diff <== computedHash - expectedHash;

    // Constraint: diff must be 0
    diff === 0;

    // If constraint passes → valid = 1
    valid <== 1;
}

component main = BidderVerification();