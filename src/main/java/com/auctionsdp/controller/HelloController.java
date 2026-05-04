package com.auctionsdp.controller;

import com.auctionsdp.model.Auction;
import com.auctionsdp.model.Bid;
import com.auctionsdp.repository.BidRepository;
import com.auctionsdp.service.AuctionService;
import com.auctionsdp.service.BidService;
import com.auctionsdp.service.RevealService;
import com.auctionsdp.service.ZkpAuctionService;
import com.auctionsdp.service.ZkpProofService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/")
public class HelloController {

    @Autowired private BidRepository bidRepository;
    @Autowired private BidService bidService;
    @Autowired private AuctionService auctionService;
    @Autowired private ZkpAuctionService zkpAuctionService;
    @Autowired private ZkpProofService zkpProofService;
    @Autowired private RevealService revealService;

    // =============================
    // HEALTH CHECK
    // =============================
    @GetMapping("/")
    public String home() {
        return "Auction Backend Running";
    }

    // =============================
    // GET ALL BIDS
    // =============================
    @GetMapping("/bids")
    public List<Bid> getAllBids() {
        return bidRepository.findAll();
    }

    // =============================
    // GET BID COUNT
    // Shows competition without revealing amounts
    // =============================
    @GetMapping("/bid-count/{auctionId}")
    public Map<String, Object> getBidCount(@PathVariable Long auctionId) {
        int count = bidService.getBidCount(auctionId);
        return Map.of(
                "auctionId", auctionId,
                "bidCount", count,
                "message", count + " sealed bids submitted — amounts hidden until reveal phase"
        );
    }

    // =============================
    // STAGE 1 AUTO BID — original circuit (baseline)
    // =============================
    @PostMapping("/auto-bid")
    public Map<String, Object> autoBid(@RequestBody Map<String, Object> request) {
        String userId        = request.get("userId").toString();
        BigInteger secret    = new BigInteger(request.get("secret").toString());
        BigInteger auctionId = new BigInteger(request.get("auctionId").toString());
        BigInteger bidNonce  = new BigInteger(request.get("bidNonce").toString());

        Map<String, Object> input = zkpAuctionService.generateProofInput(
                userId, secret, auctionId, bidNonce
        );
        return zkpProofService.generateProof(input);
    }

    // =============================
    // STAGE 2 AUTO BID — extended combined circuit
    // =============================
    @PostMapping("/auto-bid-extended")
    public Map<String, Object> autoBidExtended(@RequestBody Map<String, Object> request) {
        String userId           = request.get("userId").toString();
        BigInteger secret       = new BigInteger(request.get("secret").toString());
        BigInteger auctionId    = new BigInteger(request.get("auctionId").toString());
        BigInteger bidNonce     = new BigInteger(request.get("bidNonce").toString());
        BigInteger bidAmount    = new BigInteger(request.get("bidAmount").toString());
        BigInteger reservePrice = new BigInteger(request.get("reservePrice").toString());

        Map<String, Object> input = zkpAuctionService.generateExtendedProofInput(
                userId, secret, auctionId, bidNonce, bidAmount, reservePrice
        );

        Map<String, Object> proofData = zkpProofService.generateProof(input);

        String bidCommitment = input.get("bidCommitment").toString();
        List<Object> publicSignals = (List<Object>) proofData.get("publicSignals");
        Map<String, Object> proof = (Map<String, Object>) proofData.get("proof");

        String result = bidService.placeBidWithProof(
                proof, publicSignals,
                Long.parseLong(auctionId.toString()),
                bidCommitment
        );

        proofData.put("bidResult", result);
        proofData.put("bidCommitment", bidCommitment);
        return proofData;
    }

    // =============================
    // STAGE 3 — CLOSE AUCTION
    // Ends commit phase, starts reveal phase
    // POST /auction/1/close
    // =============================
    @PostMapping("/auction/{auctionId}/close")
    public Map<String, Object> closeAuction(@PathVariable Long auctionId) {
        return revealService.closeAuction(auctionId);
    }

    // =============================
    // STAGE 3 — REVEAL BID
    // Bidder reveals their actual amount
    // Server verifies Poseidon(bidAmount, secret) === stored commitment
    //
    // Body:
    // {
    //   "nullifier": "...",
    //   "bidAmount": "5",
    //   "bidderSecret": "12345678901234567890"
    // }
    // =============================
    @PostMapping("/reveal")
    public Map<String, Object> revealBid(@RequestBody Map<String, Object> request) {
        String nullifier        = request.get("nullifier").toString();
        BigInteger bidAmount    = new BigInteger(request.get("bidAmount").toString());
        BigInteger bidderSecret = new BigInteger(request.get("bidderSecret").toString());

        return revealService.revealBid(nullifier, bidAmount, bidderSecret);
    }

    // =============================
    // STAGE 3 — RESOLVE AUCTION
    // Finds highest revealed bid, declares winner
    // POST /auction/1/resolve
    // =============================
    @PostMapping("/auction/{auctionId}/resolve")
    public Map<String, Object> resolveAuction(@PathVariable Long auctionId) {
        return revealService.resolveAuction(auctionId);
    }

    // =============================
    // STAGE 3 — AUCTION STATUS
    // Shows current phase, bid count, winner if resolved
    // GET /auction/1/status
    // =============================
    @GetMapping("/auction/{auctionId}/status")
    public Map<String, Object> getAuctionStatus(@PathVariable Long auctionId) {
        return revealService.getAuctionStatus(auctionId);
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

    // =============================
    // GENERATE ZKP INPUT
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
    // PLACE BID — manual proof submission
    // =============================
    @PostMapping("/bid")
    public String placeBid(@RequestBody Map<String, Object> request) {
        Map<String, Object> proof = (Map<String, Object>) request.get("proof");
        List<Object> publicSignals = (List<Object>) request.get("publicSignals");

        if (proof == null || publicSignals == null) {
            throw new RuntimeException("Missing proof or publicSignals");
        }

        Long auctionId = Long.parseLong(request.get("auctionId").toString());
        String bidCommitment = request.get("bidCommitment").toString();

        return bidService.placeBidWithProof(proof, publicSignals, auctionId, bidCommitment);
    }
}