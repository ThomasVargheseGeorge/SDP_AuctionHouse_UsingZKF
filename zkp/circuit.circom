pragma circom 2.0.0;

template VerifyCredential() {
    signal input status;
    signal output valid;

    valid <== status;
}

component main = VerifyCredential();