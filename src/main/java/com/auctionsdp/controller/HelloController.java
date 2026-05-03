package com.auctionsdp.controller;

import com.auctionsdp.model.Auction;
import com.auctionsdp.model.Bid;
import com.auctionsdp.repository.BidRepository;
import com.auctionsdp.service.AuctionService;
import com.auctionsdp.service.BidService;
import com.auctionsdp.service.ZkpAuctionService;
import com.auctionsdp.service.ZkpProofService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * HelloController
 *
 * REST API for the auction system.
 *
 * Stage 1 changes:
 * - Bid endpoint now expects bidCommitment instead of plain bidAmount
 *   (bidAmount is no longer accepted in plaintext — it must be hidden)
 * - publicSignals now expected to contain nullifierPoseidon only
 *   (old insecure nullifier has been removed from circuit)
 * - Added /verify endpoint for standalone proof verification
 */
@RestController
@RequestMapping("/")
public class HelloController {

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private BidService bidService;

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private ZkpAuctionService zkpAuctionService;

    @Autowired
    private ZkpProofService zkpProofService;

    // =============================
    // HEALTH CHECK
    // =============================
    @GetMapping("/")
    public String home() {
        return "Auction Backend Running";
    }

    // =============================
    // GET ALL BIDS
    // NOTE: bid amounts are stored as commitments — not plaintext
    // =============================
    @GetMapping("/bids")
    public List<Bid> getAllBids() {
        return bidRepository.findAll();
    }

    // =============================
    // PLACE BID — manual proof submission
    //
    // Body:
    // {
    //   "proof": { ... },
    //   "publicSignals": ["valid", "nullifierPoseidon"],
    //   "auctionId": 1,
    //   "bidCommitment": "poseidon(bidAmount, bidderSecret)"  ← no plain amount
    // }
    // =============================
    @PostMapping("/bid")
    public String placeBid(@RequestBody Map<String, Object> request) {
        Map<String, Object> proof = (Map<String, Object>) request.get("proof");
        List<Object> publicSignals = (List<Object>) request.get("publicSignals");

        if (proof == null || publicSignals == null) {
            throw new RuntimeException("Missing proof or publicSignals");
        }

        Long auctionId = Long.parseLong(request.get("auctionId").toString());

        // Stage 1: bidCommitment replaces plain bidAmount
        // bidAmount is hidden — only the commitment is stored
        String bidCommitment = request.get("bidCommitment").toString();

        return bidService.placeBidWithProof(proof, publicSignals, auctionId, bidCommitment);
    }

    // =============================
    // GENERATE ZKP INPUT
    // Returns the structured input needed for proof generation
    // =============================
    @PostMapping("/zkp-input")
    public Map<String, Object> generateZkpInput(@RequestBody Map<String, Object> request) {
        String userId    = request.get("userId").toString();
        String secret    = request.get("secret").toString();
        String auctionId = request.get("auctionId").toString();
        String bidNonce  = request.get("bidNonce").toString();

        return zkpAuctionService.generateProofInput(
                userId,
                new BigInteger(secret),
                new BigInteger(auctionId),
                new BigInteger(bidNonce)
        );
    }

    // =============================
    // AUTO BID — full automated flow
    // 1. Generates ZKP input from user parameters
    // 2. Sends to Node.js server to generate proof
    // 3. Returns proof + publicSignals + timing metrics
    // =============================
    @PostMapping("/auto-bid")
    public Map<String, Object> autoBid(@RequestBody Map<String, Object> request) {
        String userId     = request.get("userId").toString();
        BigInteger secret    = new BigInteger(request.get("secret").toString());
        BigInteger auctionId = new BigInteger(request.get("auctionId").toString());
        BigInteger bidNonce  = new BigInteger(request.get("bidNonce").toString());

        // Step 1: Generate ZKP input
        Map<String, Object> input = zkpAuctionService.generateProofInput(
                userId,
                secret,
                auctionId,
                bidNonce
        );

        // Step 2: Generate proof via Node.js ZKP server
        // Returns proof, publicSignals, and timing metrics
        Map<String, Object> proofData = zkpProofService.generateProof(input);

        return proofData;
    }

    // =============================
    // CREATE AUCTION
    // =============================
    @PostMapping("/auction")
    public Auction createAuction(@RequestBody Auction auction) {
        return auctionService.createAuction(auction);
    }

    // =============================
    // GET ALL AUCTIONS
    // =============================
    @GetMapping("/auction")
    public List<Auction> getAllAuctions() {
        return auctionService.getAllAuctions();
    }
}