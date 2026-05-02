const circomlib = require("circomlibjs");

async function run() {
    const poseidon = await circomlib.buildPoseidon();

    if (process.argv.length !== 4) {
        throw new Error("Expected exactly 2 inputs");
    }

    const a = BigInt(process.argv[2]);
    const b = BigInt(process.argv[3]);

    const hash = poseidon([a, b]);
    const result = poseidon.F.toString(hash);

    console.log(result);
}

run();