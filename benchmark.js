const snarkjs = require("snarkjs");
const fs = require("fs");
const path = require("path");

const ZKP_DIR   = path.join(__dirname, "zkp");
const WASM_PATH = path.join(ZKP_DIR, "bidder_v2_js", "bidder_v2.wasm");
const ZKEY_PATH = path.join(ZKP_DIR, "circuit_v2_final.zkey");
const VKEY_PATH = path.join(ZKP_DIR, "verification_key_v2.json");

const sampleInput = {
    bidderSecret: "12345678901234567890",
    bidAmount: "5",
    pathElements: ["0","0","0","0","0","0","0","0","0","0"],
    pathIndices:  [0,0,0,0,0,0,0,0,0,0],
    auctionId:    "1",
    bidNonce:     "42",
    expectedHash: "17610922722311195426938483481431943255028223790571250909270476711880232282197",
    merkleRoot:   "17798238641302122433925557242544215882633578901871222734511163196666120141131",
    reservePrice: "2",
    bidCommitment: "7359016432285705392468442961511788192226797759108253676873103904103750856872"
};

async function benchmark(label, fn, iterations = 3) {
    const times = [];
    for (let i = 0; i < iterations; i++) {
        const start = Date.now();
        await fn();
        times.push(Date.now() - start);
    }
    const avg = (times.reduce((a,b) => a+b, 0) / times.length).toFixed(1);
    console.log(`\n${label}`);
    console.log(`  Average : ${avg} ms`);
    console.log(`  Min     : ${Math.min(...times)} ms`);
    console.log(`  Max     : ${Math.max(...times)} ms`);
    return avg;
}

async function main() {
    console.log("=".repeat(50));
    console.log("STAGE 1 BASELINE BENCHMARK");
    console.log("=".repeat(50));

    if (!fs.existsSync(WASM_PATH)) {
        console.error("WASM not found: " + WASM_PATH);
        process.exit(1);
    }

    // Proof generation time
    let lastResult;
    await benchmark("PROOF GENERATION TIME", async () => {
        lastResult = await snarkjs.groth16.fullProve(
            sampleInput, WASM_PATH, ZKEY_PATH
        );
    }, 3);

    // Proof size
    if (lastResult) {
        const sizeBytes = Buffer.byteLength(JSON.stringify(lastResult.proof), "utf8");
        console.log(`\nPROOF SIZE`);
        console.log(`  Bytes : ${sizeBytes}`);
        console.log(`  KB    : ${(sizeBytes/1024).toFixed(2)}`);
    }

    // Verification time
    if (lastResult && fs.existsSync(VKEY_PATH)) {
        const vKey = JSON.parse(fs.readFileSync(VKEY_PATH, "utf8"));
        await benchmark("PROOF VERIFICATION TIME", async () => {
            await snarkjs.groth16.verify(
                vKey, lastResult.publicSignals, lastResult.proof
            );
        }, 5);
    }

    // Scalability
    console.log("\n" + "=".repeat(50));
    console.log("SCALABILITY TEST");
    console.log("=".repeat(50));

    for (const n of [10, 50, 100]) {
        const start = Date.now();
        for (let i = 0; i < n; i++) {
            await snarkjs.groth16.fullProve(sampleInput, WASM_PATH, ZKEY_PATH);
        }
        const total = Date.now() - start;
        console.log(`\nN=${n} bidders`);
        console.log(`  Total     : ${total} ms`);
        console.log(`  Per bidder: ${(total/n).toFixed(1)} ms`);
        console.log(`  Throughput: ${(n/(total/1000)).toFixed(2)} proofs/sec`);
    }

    console.log("\n" + "=".repeat(50));
    console.log("SAVE THESE NUMBERS — Stage 2 will compare against them");
    console.log("=".repeat(50));
}

main().catch(console.error);