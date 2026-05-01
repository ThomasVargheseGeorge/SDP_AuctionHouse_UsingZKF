const express = require("express");
const { exec } = require("child_process");
const cors = require("cors");

const app = express();

app.use(cors());
app.use(express.json());

app.post("/generate-proof", (req, res) => {

    console.log("🔁 Generating proof...");

    exec(
        "snarkjs groth16 prove zkp/circuit_final.zkey zkp/witness.wtns zkp/proof.json zkp/public.json",
        (error, stdout, stderr) => {

            if (error) {
                console.error("❌ Error:", error);
                return res.json({ success: false });
            }

            console.log("✅ Proof generated");

            res.json({
                success: true
            });
        }
    );
});

app.listen(3000, () => {
    console.log("🚀 Backend running at http://localhost:3000");
});