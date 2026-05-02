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

    // 🔍 Get all bids
    @GetMapping("/bids")
    public List<Bid> getAllBids() {
        return bidRepository.findAll();
    }

    // 🚀 ZKP-based bidding (manual proof submission)
    @PostMapping("/bid")
    public String placeBid(@RequestBody Map<String, Object> request) {

        Map<String, Object> proof = (Map<String, Object>) request.get("proof");
        List<Object> publicSignals = (List<Object>) request.get("publicSignals");

        if (proof == null || publicSignals == null) {
            throw new RuntimeException("Missing proof or publicSignals");
        }

        Long auctionId = Long.parseLong(request.get("auctionId").toString());
        Double bidAmount = Double.parseDouble(request.get("bidAmount").toString());

        return bidService.placeBidWithProof(proof, publicSignals, auctionId, bidAmount);
    }

    // 🆕 Generate ZKP input (for manual flow if needed)
    @PostMapping("/zkp-input")
    public Map<String, Object> generateZkpInput(@RequestBody Map<String, Object> request) {

        String userId = request.get("userId").toString();
        String secret = request.get("secret").toString();
        String auctionId = request.get("auctionId").toString();
        String bidNonce = request.get("bidNonce").toString();

        return zkpAuctionService.generateProofInput(
                userId,
                new BigInteger(secret),
                new BigInteger(auctionId),
                new BigInteger(bidNonce)
        );
    }

    // 🚀 FULL AUTOMATED FLOW (NO CLI NEEDED)
    @PostMapping("/auto-bid")
    public Map<String, Object> autoBid(@RequestBody Map<String, Object> request) {

        String userId = request.get("userId").toString();
        BigInteger secret = new BigInteger(request.get("secret").toString());
        BigInteger auctionId = new BigInteger(request.get("auctionId").toString());
        BigInteger bidNonce = new BigInteger(request.get("bidNonce").toString());

        // 1. Generate ZKP input
        Map<String, Object> input = zkpAuctionService.generateProofInput(
                userId,
                secret,
                auctionId,
                bidNonce
        );

        // 2. Generate proof automatically
        Map<String, Object> proofData = zkpProofService.generateProof(input);

        return proofData;
    }

    // 🏷️ Create auction
    @PostMapping("/auction")
    public Auction createAuction(@RequestBody Auction auction) {
        return auctionService.createAuction(auction);
    }

    // 📊 Get auctions
    @GetMapping("/auction")
    public List<Auction> getAllAuctions() {
        return auctionService.getAllAuctions();
    }

    // 🏠 Health check
    @GetMapping("/")
    public String home() {
        return "Auction Backend Running 🚀";
    }
}