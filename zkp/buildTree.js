const circomlib = require("circomlibjs");

async function run() {
    const poseidon = await circomlib.buildPoseidon();

    function hash1(a) {
        const res = poseidon([BigInt(a)]);
        return poseidon.F.toString(res);
    }

    function hash2(a, b) {
        const res = poseidon([BigInt(a), BigInt(b)]);
        return poseidon.F.toString(res);
    }

    const depth = 10;

    // 🔹 Test secrets
    const secret = 11;
    const otherSecret = 22;

    // 🔹 Leaves
    const leaf0 = hash1(secret);
    const leaf1 = hash1(otherSecret);

    console.log("leaf0:", leaf0);
    console.log("leaf1:", leaf1);

    // 🔹 Build root (FULL depth)
    let current = hash2(leaf0, leaf1);

    for (let i = 1; i < depth; i++) {
        current = hash2(current, 0);
    }

    const merkleRoot = current;

    console.log("merkleRoot:", merkleRoot);

    // 🔹 Path (index 0)
    let pathElements = [leaf1];
    let pathIndices = [0];

    while (pathElements.length < depth) {
        pathElements.push("0");
        pathIndices.push(0);
    }

    console.log("pathElements:", JSON.stringify(pathElements));
    console.log("pathIndices:", JSON.stringify(pathIndices));
}

run();