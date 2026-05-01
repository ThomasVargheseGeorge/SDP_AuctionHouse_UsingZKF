package com.auctionsdp.controller;

import com.auctionsdp.model.Auction;
import com.auctionsdp.model.Bid;
import com.auctionsdp.repository.BidRepository;
import com.auctionsdp.service.AuctionService;
import com.auctionsdp.service.BidService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    // 🔍 Get all bids
    @GetMapping("/bids")
    public List<Bid> getAllBids() {
        return bidRepository.findAll();
    }

    // 🚀 ZKP-based bidding (UPDATED)
    @PostMapping("/bid")
    public String placeBid(@RequestBody Map<String, Object> request) {

        Map<String, Object> proof = (Map<String, Object>) request.get("proof");
        List<Object> publicSignals = (List<Object>) request.get("publicSignals");

        Long auctionId = Long.parseLong(request.get("auctionId").toString());
        Double bidAmount = Double.parseDouble(request.get("bidAmount").toString());

        return bidService.placeBidWithProof(proof, publicSignals, auctionId, bidAmount);
    }

    // 🏠 Health check
    @GetMapping("/")
    public String home() {
        return "Auction Backend Running 🚀";
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
}