const circomlib = require("circomlibjs");

async function run() {
    const poseidon = await circomlib.buildPoseidon();
    const F = poseidon.F;

    const secret = 5;
    const hash = poseidon([secret]);

    console.log(F.toString(hash));
}

run();