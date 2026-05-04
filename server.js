const express = require("express");
const cors = require("cors");
const fs = require("fs");
const path = require("path");
const snarkjs = require("snarkjs");

const app = express();
app.use(cors());
app.use(express.json());

// =============================
// PATHS
// All ZKP files live in the zkp/ folder
// =============================
const ZKP_DIR       = path.join(__dirname, "zkp");
const WASM_PATH     = path.join(ZKP_DIR, "bidder_v2_js", "bidder_v2.wasm");
const ZKEY_PATH     = path.join(ZKP_DIR, "circuit_v2_final.zkey");
const VKEY_PATH     = path.join(ZKP_DIR, "verification_key_v2.json");

// =============================
// HEALTH CHECK
// =============================
app.get("/", (req, res) => {
    res.json({ status: "ZKP Server running", timestamp: Date.now() });
});

// =============================
// GENERATE PROOF
// Accepts circuit inputs as JSON
// Returns proof, publicSignals, and timing metrics
//
// Expected body:
// {
//   bidderSecret: "123456",
//   pathElements: ["0","0",...],   // 10 elements
//   pathIndices:  [0, 0, ...],     // 10 elements
//   auctionId:    "1",
//   bidNonce:     "42",
//   expectedHash: "...",
//   merkleRoot:   "..."
// }
// =============================
app.post("/generate-proof", async (req, res) => {
    console.log("Generating proof...");

    const input = req.body;

    // Validate required fields
    const required = [
        "bidderSecret", "pathElements", "pathIndices",
        "auctionId", "bidNonce", "expectedHash", "merkleRoot"
    ];
    for (const field of required) {
        if (input[field] === undefined) {
            return res.status(400).json({
                success: false,
                error: `Missing field: ${field}`
            });
        }
    }

    try {
        // Measure proof generation time
        const startTime = Date.now();

        const { proof, publicSignals } = await snarkjs.groth16.fullProve(
            input,
            WASM_PATH,
            ZKEY_PATH
        );

        const proofGenerationTime = Date.now() - startTime;

        // Measure proof size
        const proofJson = JSON.stringify(proof);
        const proofSizeBytes = Buffer.byteLength(proofJson, "utf8");

        console.log(`Proof generated in ${proofGenerationTime}ms, size: ${proofSizeBytes} bytes`);

        res.json({
            success: true,
            proof,
            publicSignals,
            metrics: {
                proofGenerationTimeMs: proofGenerationTime,
                proofSizeBytes: proofSizeBytes
            }
        });

    } catch (error) {
        console.error("Proof generation failed:", error.message);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

// =============================
// VERIFY PROOF
// Accepts proof and publicSignals
// Returns verification result and timing
//
// Expected body:
// {
//   proof: { ... },
//   publicSignals: ["...", "..."]
// }
// =============================
app.post("/verify-proof", async (req, res) => {
    console.log("Verifying proof...");

    const { proof, publicSignals } = req.body;

    if (!proof || !publicSignals) {
        return res.status(400).json({
            success: false,
            error: "Missing proof or publicSignals"
        });
    }

    try {
        // Load verification key
        const vKey = JSON.parse(fs.readFileSync(VKEY_PATH, "utf8"));

        // Measure verification time
        const startTime = Date.now();

        const isValid = await snarkjs.groth16.verify(vKey, publicSignals, proof);

        const verificationTime = Date.now() - startTime;

        console.log(`Proof verified: ${isValid} in ${verificationTime}ms`);

        res.json({
            success: true,
            valid: isValid,
            metrics: {
                verificationTimeMs: verificationTime
            }
        });

    } catch (error) {
        console.error("Verification failed:", error.message);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

// =============================
// GENERATE + VERIFY IN ONE CALL
// Convenience endpoint — generates proof then immediately verifies it
// Returns all metrics together
// Used by the Java backend's auto-bid flow
// =============================
app.post("/full-prove", async (req, res) => {
    console.log("Full prove and verify...");

    const input = req.body;
    console.log("INPUT RECEIVED:", JSON.stringify(input, null, 2));

    try {
        // STEP 1: Generate proof
        const genStart = Date.now();

        const { proof, publicSignals } = await snarkjs.groth16.fullProve(
            input,
            WASM_PATH,
            ZKEY_PATH
        );

        const proofGenerationTime = Date.now() - genStart;

        // STEP 2: Verify proof
        const vKey = JSON.parse(fs.readFileSync(VKEY_PATH, "utf8"));

        const verifyStart = Date.now();
        const isValid = await snarkjs.groth16.verify(vKey, publicSignals, proof);
        const verificationTime = Date.now() - verifyStart;

        // STEP 3: Measure proof size
        const proofSizeBytes = Buffer.byteLength(JSON.stringify(proof), "utf8");

        console.log(`Full prove complete — valid: ${isValid}, gen: ${proofGenerationTime}ms, verify: ${verificationTime}ms`);

        res.json({
            success: true,
            valid: isValid,
            proof,
            publicSignals,
            metrics: {
                proofGenerationTimeMs: proofGenerationTime,
                verificationTimeMs: verificationTime,
                proofSizeBytes: proofSizeBytes,
                totalTimeMs: proofGenerationTime + verificationTime
            }
        });

    } catch (error) {
        console.error("Full prove failed:", error.message);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

// =============================
// START SERVER
// =============================
const PORT = 3000;
app.listen(PORT, () => {
    console.log(`ZKP Server running at http://localhost:${PORT}`);
    console.log(`WASM: ${WASM_PATH}`);
    console.log(`ZKEY: ${ZKEY_PATH}`);
    console.log(`VKEY: ${VKEY_PATH}`);
});