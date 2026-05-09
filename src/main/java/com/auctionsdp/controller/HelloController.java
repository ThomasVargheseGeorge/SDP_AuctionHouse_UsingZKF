package com.auctionsdp.controller;

import com.auctionsdp.model.Auction;
import com.auctionsdp.model.Bid;
import com.auctionsdp.model.NFT;
import com.auctionsdp.repository.BidRepository;
import com.auctionsdp.service.AuctionService;
import com.auctionsdp.service.BidService;
import com.auctionsdp.service.NFTService;
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
    @Autowired private NFTService nftService;

    @GetMapping("/")
    public String home() {
        return "Auction Backend Running";
    }

    @GetMapping("/bids")
    public List<Bid> getAllBids() {
        return bidRepository.findAll();
    }

    
    @GetMapping("/bid-count/{auctionId}")
    public Map<String, Object> getBidCount(@PathVariable Long auctionId) {
        int count = bidService.getBidCount(auctionId);
        return Map.of(
                "auctionId", auctionId,
                "bidCount", count,
                "message", count + " sealed bids submitted — amounts hidden until reveal phase"
        );
    }

    
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

    
    @PostMapping("/auction/{auctionId}/close")
    public Map<String, Object> closeAuction(@PathVariable Long auctionId) {
        return revealService.closeAuction(auctionId);
    }

    
    @PostMapping("/reveal")
    public Map<String, Object> revealBid(@RequestBody Map<String, Object> request) {
        String nullifier        = request.get("nullifier").toString();
        BigInteger bidAmount    = new BigInteger(request.get("bidAmount").toString());
        BigInteger bidderSecret = new BigInteger(request.get("bidderSecret").toString());
        return revealService.revealBid(nullifier, bidAmount, bidderSecret);
    }

    
    @PostMapping("/auction/{auctionId}/resolve")
    public Map<String, Object> resolveAuction(@PathVariable Long auctionId) {
        return revealService.resolveAuction(auctionId);
    }

    
    @GetMapping("/auction/{auctionId}/status")
    public Map<String, Object> getAuctionStatus(@PathVariable Long auctionId) {
        return revealService.getAuctionStatus(auctionId);
    }

    
    @PostMapping("/auction")
    public Auction createAuction(@RequestBody Auction auction) {
        return auctionService.createAuction(auction);
    }
    @GetMapping("/auction")
    public List<Auction> getAllAuctions() {
        return auctionService.getAllAuctions();
    }

    @PostMapping("/nft/mint")
    public NFT mintNFT(@RequestBody Map<String, Object> request) {
        String tokenId         = request.get("tokenId").toString();
        String name            = request.get("name").toString();
        String description     = request.get("description").toString();
        String ownerNullifier  = request.get("ownerNullifier").toString();
        return nftService.mintNFT(tokenId, name, description, ownerNullifier);
    }

    
    @PostMapping("/nft/list")
    public Map<String, Object> listNFT(@RequestBody Map<String, Object> request) {
        String tokenId  = request.get("tokenId").toString();
        Long auctionId  = Long.parseLong(request.get("auctionId").toString());
        return nftService.listNFTForAuction(tokenId, auctionId);
    }

    
    @GetMapping("/nft/{tokenId}")
    public NFT getNFT(@PathVariable String tokenId) {
        return nftService.getNFT(tokenId);
    }

    
    @GetMapping("/nft")
    public List<NFT> getAllNFTs() {
        return nftService.getAllNFTs();
    }

    
    @GetMapping("/auction/{auctionId}/summary")
    public Map<String, Object> getAuctionSummary(@PathVariable Long auctionId) {
        return nftService.getAuctionNFTSummary(auctionId);
    }

    
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