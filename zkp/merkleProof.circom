pragma circom 2.0.0;

include "node_modules/circomlib/circuits/poseidon.circom";

template MerkleProof(depth) {

    signal input leaf;
    signal input pathElements[depth];
    signal input pathIndices[depth];

    signal output root;

    signal hashes[depth + 1];

    // helper signals (ALL declared upfront)
    signal leftA[depth];
    signal leftB[depth];
    signal rightA[depth];
    signal rightB[depth];

    hashes[0] <== leaf;

    component poseidons[depth];

    for (var i = 0; i < depth; i++) {

        poseidons[i] = Poseidon(2);

        // break into quadratic-safe steps

        leftA[i] <== (1 - pathIndices[i]) * hashes[i];
        leftB[i] <== pathIndices[i] * pathElements[i];

        rightA[i] <== pathIndices[i] * hashes[i];
        rightB[i] <== (1 - pathIndices[i]) * pathElements[i];

        poseidons[i].inputs[0] <== leftA[i] + leftB[i];
        poseidons[i].inputs[1] <== rightA[i] + rightB[i];

        hashes[i + 1] <== poseidons[i].out;
    }

    root <== hashes[depth];
}