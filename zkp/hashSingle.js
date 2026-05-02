const circomlib = require("circomlibjs");

async function run() {
    const poseidon = await circomlib.buildPoseidon();

    if (process.argv.length !== 3) {
        throw new Error("Expected exactly 1 input");
    }

    const a = BigInt(process.argv[2]);

    const hash = poseidon([a]);
    const result = poseidon.F.toString(hash);

    console.log(result);
}

run();