package com.auctionsdp.service;

import com.auctionsdp.model.Auction;
import com.auctionsdp.model.Bid;
import com.auctionsdp.repository.AuctionRepository;
import com.auctionsdp.repository.BidRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * BidService
 *
 * Stage 1 complete rewrite:
 *
 * WHAT CHANGED:
 * - bidAmount (Double plaintext) completely removed from bid placement
 * - bidCommitment (String) is now what gets stored — Poseidon(bidAmount, bidderSecret)
 * - Nullifier check moved to database (existsByNullifier) instead of in-memory Set
 *   in-memory Set resets on server restart, database does not
 * - ZKP verification now cross-platform (works on Windows and Linux/Mac)
 * - Old SHA-256 hashBidder method removed — not used in ZKP flow
 * - Auction highest bid no longer updated during commit phase
 *   highest bid is only known after reveal phase (Stage 4)
 *
 * WHAT STAYS THE SAME:
 * - ZKP verification via snarkjs CLI
 * - Auction active check
 * - Anonymous bidderId
 */
@Service
public class BidService {

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    // =============================
    // PLACE BID WITH PROOF — COMMIT PHASE
    //
    // Accepts a ZKP proof and a bid commitment.
    // Does NOT accept or store the plain bid amount.
    // The amount stays hidden until the reveal phase.
    //
    // publicSignals order from circuit: [valid, nullifierPoseidon]
    // =============================
    public String placeBidWithProof(
            Map<String, Object> proof,
            List<Object> publicSignals,
            Long auctionId,
            String bidCommitment
    ) {
        // Step 1: Basic input validation
        if (proof == null || publicSignals == null || auctionId == null || bidCommitment == null) {
            return "Invalid request data";
        }

        // Step 2: Validate publicSignals has both outputs
        // Index 0 = valid, Index 1 = nullifierPoseidon
        if (publicSignals.size() < 2) {
            return "Invalid public signals — expected [valid, nullifierPoseidon]";
        }

        // Step 3: Extract secure Poseidon nullifier
        String nullifier = publicSignals.get(1).toString();

        // Step 4: Check nullifier not already used — database-level check
        // Survives server restarts unlike the old in-memory Set
        if (bidRepository.existsByNullifier(nullifier)) {
            return "Double bidding detected — nullifier already used";
        }

        // Step 5: Verify ZKP proof via snarkjs
        boolean isValid = verifyProofExternally(proof, publicSignals);
        if (!isValid) {
            return "Invalid ZKP proof — bid rejected";
        }

        // Step 6: Check auction exists and is active
        Optional<Auction> auctionOpt = auctionRepository.findById(auctionId);
        if (auctionOpt.isEmpty()) {
            return "Auction not found";
        }

        Auction auction = auctionOpt.get();
        if (!auction.isActive()) {
            return "Auction is closed";
        }

        // Step 7: Store bid commitment
        // We do NOT update currentHighestBid here
        // That only happens after reveal phase when actual amounts are known
        Bid bid = new Bid();
        bid.setAuctionId(auctionId);
        bid.setBidCommitment(bidCommitment);
        bid.setNullifier(nullifier);
        bid.setRevealed(false);
        bid.setBidderId("ZKP_VERIFIED");

        bidRepository.save(bid);

        return "Bid committed successfully — amount hidden until reveal phase";
    }

    // =============================
    // GET ALL BIDS FOR AUCTION
    // =============================
    public List<Bid> getBidsForAuction(Long auctionId) {
        return bidRepository.findByAuctionId(auctionId);
    }

    // =============================
    // GET BID COUNT FOR AUCTION
    // Used by frontend to show "N bids submitted" without revealing amounts
    // =============================
    public int getBidCount(Long auctionId) {
        return bidRepository.findByAuctionId(auctionId).size();
    }

    // =============================
    // ZKP VERIFICATION — cross platform
    // Works on both Windows (cmd) and Linux/Mac (bash)
    // =============================
    private boolean verifyProofExternally(Map<String, Object> proof, List<Object> publicSignals) {
        try {
            String zkpPath = "zkp/";

            String tempProofPath  = zkpPath + "temp_proof.json";
            String tempPublicPath = zkpPath + "temp_public.json";

            mapper.writeValue(new File(tempProofPath), proof);
            mapper.writeValue(new File(tempPublicPath), publicSignals);

            boolean isWindows = System.getProperty("os.name")
                    .toLowerCase().contains("win");

            ProcessBuilder processBuilder;
            if (isWindows) {
                processBuilder = new ProcessBuilder(
                        "cmd", "/c",
                        "snarkjs", "groth16", "verify",
                        zkpPath + "verification_key.json",
                        tempPublicPath,
                        tempProofPath
                );
            } else {
                processBuilder = new ProcessBuilder(
                        "snarkjs", "groth16", "verify",
                        zkpPath + "verification_key.json",
                        tempPublicPath,
                        tempProofPath
                );
            }

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("snarkjs: " + line);
                if (line.contains("OK")) {
                    return true;
                }
            }

            process.waitFor();
            return false;

        } catch (Exception e) {
            System.err.println("ZKP verification error: " + e.getMessage());
            return false;
        }
    }
}