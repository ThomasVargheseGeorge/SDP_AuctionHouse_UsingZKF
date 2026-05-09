/**
 * benchmark_comparison.js
 *
 * Stage 5 — Full Benchmarking Script
 *
 * Compares three approaches:
 * 1. Baseline (Stage 1) — identity + merkle only
 * 2. Combined (Stage 2) — identity + merkle + range + commitment in ONE proof
 * 3. Naive (simulated) — two separate proofs for range and commitment
 *
 * Outputs:
 * - Console table of all metrics
 * - benchmark_results.json — used by frontend Chart.js graphs
 *
 * Usage:
 *   node benchmark_comparison.js
 */

const snarkjs = require("snarkjs");
const fs = require("fs");
const path = require("path");

const ZKP_DIR = path.join(__dirname, "zkp");

const CIRCUITS = {
    baseline: {
        wasm: path.join(ZKP_DIR, "bidder_js", "bidder.wasm"),
        zkey: path.join(ZKP_DIR, "circuit_final.zkey"),
        vkey: path.join(ZKP_DIR, "verification_key.json"),
        label: "Stage 1 Baseline"
    },
    combined: {
        wasm: path.join(ZKP_DIR, "bidder_v2_js", "bidder_v2.wasm"),
        zkey: path.join(ZKP_DIR, "circuit_v2_final.zkey"),
        vkey: path.join(ZKP_DIR, "verification_key_v2.json"),
        label: "Stage 2 Combined"
    }
};

// =============================
// INPUTS
// Use real values from your working system
// =============================
const BASELINE_INPUT = {
    bidderSecret: "12345678901234567890",
    pathElements: ["0","0","0","0","0","0","0","0","0","0"],
    pathIndices:  [0,0,0,0,0,0,0,0,0,0],
    auctionId:    "1",
    bidNonce:     "42",
    expectedHash: "17610922722311195426938483481431943255028223790571250909270476711880232282197",
    merkleRoot:   "17798238641302122433925557242544215882633578901871222734511163196666120141131"
};

const COMBINED_INPUT = {
    bidderSecret:  "12345678901234567890",
    bidAmount:     "5",
    pathElements:  ["0","0","0","0","0","0","0","0","0","0"],
    pathIndices:   [0,0,0,0,0,0,0,0,0,0],
    auctionId:     "1",
    bidNonce:      "42",
    expectedHash:  "17610922722311195426938483481431943255028223790571250909270476711880232282197",
    merkleRoot:    "17798238641302122433925557242544215882633578901871222734511163196666120141131",
    reservePrice:  "2",
    bidCommitment: "7359016432285705392468442961511788192226797759108253676873103904103750856872"
};

// =============================
// BENCHMARK RUNNER
// =============================
async function runBenchmark(label, input, circuit, iterations = 5) {
    console.log(`\nBenchmarking: ${label}`);

    const genTimes = [];
    const verifyTimes = [];
    let proofSizeBytes = 0;
    let lastProof, lastPublicSignals;

    const vKey = JSON.parse(fs.readFileSync(circuit.vkey, "utf8"));

    for (let i = 0; i < iterations; i++) {
        process.stdout.write(`  Run ${i+1}/${iterations}...`);

        // Proof generation
        const genStart = Date.now();
        const { proof, publicSignals } = await snarkjs.groth16.fullProve(
            input, circuit.wasm, circuit.zkey
        );
        const genTime = Date.now() - genStart;
        genTimes.push(genTime);

        // Proof size
        proofSizeBytes = Buffer.byteLength(JSON.stringify(proof), "utf8");

        // Verification
        const verifyStart = Date.now();
        await snarkjs.groth16.verify(vKey, publicSignals, proof);
        const verifyTime = Date.now() - verifyStart;
        verifyTimes.push(verifyTime);

        lastProof = proof;
        lastPublicSignals = publicSignals;

        console.log(` gen: ${genTime}ms, verify: ${verifyTime}ms`);
    }

    const avg = arr => (arr.reduce((a,b) => a+b, 0) / arr.length);
    const min = arr => Math.min(...arr);
    const max = arr => Math.max(...arr);

    return {
        label,
        proofGenerationAvg: Math.round(avg(genTimes)),
        proofGenerationMin: min(genTimes),
        proofGenerationMax: max(genTimes),
        verificationAvg: Math.round(avg(verifyTimes)),
        verificationMin: min(verifyTimes),
        verificationMax: max(verifyTimes),
        proofSizeBytes,
        proofSizeKB: (proofSizeBytes / 1024).toFixed(2)
    };
}

// =============================
// SCALABILITY TEST
// =============================
async function runScalability(label, input, circuit, counts = [10, 50, 100, 200]) {
    console.log(`\nScalability test: ${label}`);
    const results = [];

    for (const n of counts) {
        process.stdout.write(`  N=${n}...`);
        const start = Date.now();

        for (let i = 0; i < n; i++) {
            await snarkjs.groth16.fullProve(input, circuit.wasm, circuit.zkey);
        }

        const total = Date.now() - start;
        const perBidder = total / n;
        const throughput = parseFloat((n / (total / 1000)).toFixed(2));

        console.log(` ${total}ms total, ${perBidder.toFixed(1)}ms/bidder, ${throughput} proofs/sec`);

        results.push({ n, totalMs: total, perBidderMs: parseFloat(perBidder.toFixed(1)), throughput });
    }

    return results;
}

// =============================
// NAIVE SIMULATION
// Simulates running two separate proofs (range + commitment)
// by doubling the baseline circuit time
// This is what the naive approach would cost
// =============================
function simulateNaive(baselineResult, scalability) {
    return {
        label: "Naive Two-Proof",
        proofGenerationAvg: baselineResult.proofGenerationAvg * 2,
        proofGenerationMin: baselineResult.proofGenerationMin * 2,
        proofGenerationMax: baselineResult.proofGenerationMax * 2,
        verificationAvg: baselineResult.verificationAvg * 2,
        verificationMin: baselineResult.verificationMin * 2,
        verificationMax: baselineResult.verificationMax * 2,
        proofSizeBytes: baselineResult.proofSizeBytes * 2,
        proofSizeKB: (baselineResult.proofSizeBytes * 2 / 1024).toFixed(2),
        scalability: scalability.map(s => ({
            ...s,
            totalMs: s.totalMs * 2,
            perBidderMs: s.perBidderMs * 2,
            throughput: parseFloat((s.throughput / 2).toFixed(2))
        }))
    };
}

// =============================
// PRINT RESULTS TABLE
// =============================
function printTable(results) {
    console.log("\n" + "=".repeat(70));
    console.log("BENCHMARK RESULTS SUMMARY");
    console.log("=".repeat(70));
    console.log(
        "Metric".padEnd(30),
        "Baseline".padEnd(15),
        "Naive 2x".padEnd(15),
        "Combined"
    );
    console.log("-".repeat(70));

    const [baseline, naive, combined] = results;

    const rows = [
        ["Proof Gen (avg ms)", baseline.proofGenerationAvg, naive.proofGenerationAvg, combined.proofGenerationAvg],
        ["Proof Gen (min ms)", baseline.proofGenerationMin, naive.proofGenerationMin, combined.proofGenerationMin],
        ["Verification (avg ms)", baseline.verificationAvg, naive.verificationAvg, combined.verificationAvg],
        ["Proof Size (bytes)", baseline.proofSizeBytes, naive.proofSizeBytes, combined.proofSizeBytes],
        ["Proof Size (KB)", baseline.proofSizeKB, naive.proofSizeKB, combined.proofSizeKB],
        ["Constraints", 6251, 12502, 6836],
        ["Properties proved", 2, 4, 4]
    ];

    for (const row of rows) {
        console.log(
            row[0].toString().padEnd(30),
            row[1].toString().padEnd(15),
            row[2].toString().padEnd(15),
            row[3].toString()
        );
    }

    console.log("=".repeat(70));

    const improvement = (((naive.proofGenerationAvg - combined.proofGenerationAvg) / naive.proofGenerationAvg) * 100).toFixed(1);
    console.log(`\nKey finding: Combined circuit is ${improvement}% faster than naive two-proof approach`);
    console.log(`while proving the same 4 properties.`);
}

// =============================
// MAIN
// =============================
async function main() {
    console.log("=".repeat(70));
    console.log("STAGE 5 BENCHMARK — Baseline vs Naive vs Combined");
    console.log("=".repeat(70));

    // Check files exist
    for (const [key, circuit] of Object.entries(CIRCUITS)) {
        if (!fs.existsSync(circuit.wasm)) {
            console.error(`Missing WASM for ${key}: ${circuit.wasm}`);
            process.exit(1);
        }
    }

    // Run benchmarks
    const baselineResult = await runBenchmark(
        "Stage 1 Baseline", BASELINE_INPUT, CIRCUITS.baseline, 5
    );

    const combinedResult = await runBenchmark(
        "Stage 2 Combined", COMBINED_INPUT, CIRCUITS.combined, 5
    );

    // Scalability
    console.log("\n--- Scalability Tests ---");
    const baselineScale = await runScalability(
        "Baseline", BASELINE_INPUT, CIRCUITS.baseline, [10, 50, 100]
    );
    const combinedScale = await runScalability(
        "Combined", COMBINED_INPUT, CIRCUITS.combined, [10, 50, 100]
    );

    // Simulate naive
    const naiveResult = simulateNaive(baselineResult, baselineScale);

    // Print table
    printTable([baselineResult, naiveResult, combinedResult]);

    // Save results for frontend graphs
    const output = {
        timestamp: new Date().toISOString(),
        circuits: {
            baseline: {
                ...baselineResult,
                constraints: 6251,
                propertiesProved: 2,
                scalability: baselineScale
            },
            naive: {
                ...naiveResult,
                constraints: 12502,
                propertiesProved: 4,
                note: "Simulated — baseline x2"
            },
            combined: {
                ...combinedResult,
                constraints: 6836,
                propertiesProved: 4,
                scalability: combinedScale
            }
        },
        summary: {
            improvementOverNaive: parseFloat(
                (((naiveResult.proofGenerationAvg - combinedResult.proofGenerationAvg) /
                naiveResult.proofGenerationAvg) * 100).toFixed(1)
            ),
            constraintReduction: parseFloat(
                (((12502 - 6836) / 12502) * 100).toFixed(1)
            )
        }
    };

    fs.writeFileSync("benchmark_results.json", JSON.stringify(output, null, 2));
    console.log("\nResults saved to benchmark_results.json");
    console.log("Use this file for Chart.js graphs in Stage 6 frontend.");
}

main().catch(console.error);