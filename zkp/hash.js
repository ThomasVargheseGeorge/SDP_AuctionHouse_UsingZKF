const circomlib = require("circomlibjs");

async function run() {
    const poseidon = await circomlib.buildPoseidon();

    const inputs = process.argv.slice(2).map(x => BigInt(x));

    if (inputs.length === 0) {
        console.log("No inputs provided");
        return;
    }

    const hash = poseidon(inputs);
    const result = poseidon.F.toString(hash);

    console.log(result);
}

run();