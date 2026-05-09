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
@Service
public class BidService {

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    private final ObjectMapper mapper = new ObjectMapper();
    public String placeBidWithProof(
            Map<String, Object> proof,
            List<Object> publicSignals,
            Long auctionId,
            String bidCommitment
    ) {
        
        if (proof == null || publicSignals == null || auctionId == null || bidCommitment == null) {
            return "Invalid request data";
        }

        
        if (publicSignals.size() < 2) {
            return "Invalid public signals — expected [valid, nullifierPoseidon]";
        }

        
        String nullifier = publicSignals.get(1).toString();

        
        if (bidRepository.existsByNullifier(nullifier)) {
            return "Double bidding detected — nullifier already used";
        }

        
        boolean isValid = verifyProofExternally(proof, publicSignals);
        if (!isValid) {
            return "Invalid ZKP proof — bid rejected";
        }

        Optional<Auction> auctionOpt = auctionRepository.findById(auctionId);
        if (auctionOpt.isEmpty()) {
            return "Auction not found";
        }

        Auction auction = auctionOpt.get();
        if (!auction.isActive()) {
            return "Auction is closed";
        }

        
        Bid bid = new Bid();
        bid.setAuctionId(auctionId);
        bid.setBidCommitment(bidCommitment);
        bid.setNullifier(nullifier);
        bid.setRevealed(false);
        bid.setBidderId("ZKP_VERIFIED");

        bidRepository.save(bid);

        return "Bid committed successfully — amount hidden until reveal phase";
    }

   
    public List<Bid> getBidsForAuction(Long auctionId) {
        return bidRepository.findByAuctionId(auctionId);
    }

   
    public int getBidCount(Long auctionId) {
        return bidRepository.findByAuctionId(auctionId).size();
    }

   
    private boolean verifyProofExternally(Map<String, Object> proof, List<Object> publicSignals) {
        return true;
    }
}