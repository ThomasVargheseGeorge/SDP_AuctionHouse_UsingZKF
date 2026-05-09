package com.auctionsdp.service;

import com.auctionsdp.model.Auction;
import com.auctionsdp.model.Bid;
import com.auctionsdp.repository.AuctionRepository;
import com.auctionsdp.repository.BidRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Service
public class RevealService {

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private NFTService nftService;


    public Map<String, Object> closeAuction(Long auctionId) {
        Optional<Auction> auctionOpt = auctionRepository.findById(auctionId);
        if (auctionOpt.isEmpty()) {
            throw new RuntimeException("Auction not found: " + auctionId);
        }

        Auction auction = auctionOpt.get();
        auction.setActive(false);
        auction.setRevealPhase(true);
        auctionRepository.save(auction);

        int bidCount = bidRepository.findByAuctionId(auctionId).size();

        return Map.of(
                "message", "Auction closed — reveal phase started",
                "auctionId", auctionId,
                "totalBids", bidCount,
                "revealPhase", true
        );
    }

 
    public Map<String, Object> revealBid(
            String nullifier,
            BigInteger bidAmount,
            BigInteger bidderSecret
    ) {
        Optional<Bid> bidOpt = bidRepository.findByNullifier(nullifier);
        if (bidOpt.isEmpty()) {
            throw new RuntimeException("Bid not found for nullifier: " + nullifier);
        }

        Bid bid = bidOpt.get();

        if (bid.isRevealed()) {
            throw new RuntimeException("Bid already revealed");
        }

        Optional<Auction> auctionOpt = auctionRepository.findById(bid.getAuctionId());
        if (auctionOpt.isEmpty()) {
            throw new RuntimeException("Auction not found");
        }

        Auction auction = auctionOpt.get();
        if (!auction.isRevealPhase()) {
            throw new RuntimeException("Auction is not in reveal phase yet");
        }

    
        BigInteger computedCommitment = PoseidonHash.hash(bidAmount, bidderSecret);
        String storedCommitment = bid.getBidCommitment();

        if (!computedCommitment.toString().equals(storedCommitment)) {
            throw new RuntimeException(
                "Commitment mismatch — revealed amount does not match committed value"
            );
        }

        bid.setRevealed(true);
        bid.setRevealedAmount(bidAmount.doubleValue());
        bidRepository.save(bid);

        return Map.of(
                "message", "Bid revealed successfully",
                "nullifier", nullifier,
                "revealedAmount", bidAmount.doubleValue(),
                "valid", true
        );
    }

   
    public Map<String, Object> resolveAuction(Long auctionId) {
        Optional<Auction> auctionOpt = auctionRepository.findById(auctionId);
        if (auctionOpt.isEmpty()) {
            throw new RuntimeException("Auction not found: " + auctionId);
        }

        Auction auction = auctionOpt.get();
        if (!auction.isRevealPhase()) {
            throw new RuntimeException("Auction must be in reveal phase to resolve");
        }

        List<Bid> revealedBids = bidRepository.findByAuctionIdAndRevealed(auctionId, true);
        if (revealedBids.isEmpty()) {
            throw new RuntimeException("No revealed bids found for auction: " + auctionId);
        }

       
        Bid winningBid = revealedBids.get(0);
        for (Bid bid : revealedBids) {
            if (bid.getRevealedAmount() > winningBid.getRevealedAmount()) {
                winningBid = bid;
            }
        }

        
        auction.setWinnerNullifier(winningBid.getNullifier());
        auction.setWinnerAmount(winningBid.getRevealedAmount());
        auction.setCurrentHighestBid(winningBid.getRevealedAmount());
        auction.setRevealPhase(false);
        auctionRepository.save(auction);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Auction resolved — winner determined");
        result.put("auctionId", auctionId);
        result.put("winnerNullifier", winningBid.getNullifier());
        result.put("winningAmount", winningBid.getRevealedAmount());
        result.put("totalRevealed", revealedBids.size());

        
        if (auction.getNftTokenId() != null) {
            try {
                Map<String, Object> transferResult = nftService.transferNFTToWinner(auctionId);
                result.put("nftTransfer", transferResult);
                result.put("nftTransferred", true);
            } catch (Exception e) {
                // NFT transfer failed — auction still resolved
                result.put("nftTransferred", false);
                result.put("nftTransferError", e.getMessage());
            }
        } else {
            result.put("nftTransferred", false);
            result.put("nftTransferNote", "No NFT linked to this auction");
        }

        return result;
    }

    
    public Map<String, Object> getAuctionStatus(Long auctionId) {
        Optional<Auction> auctionOpt = auctionRepository.findById(auctionId);
        if (auctionOpt.isEmpty()) {
            throw new RuntimeException("Auction not found: " + auctionId);
        }

        Auction auction = auctionOpt.get();
        List<Bid> allBids = bidRepository.findByAuctionId(auctionId);
        List<Bid> revealedBids = bidRepository.findByAuctionIdAndRevealed(auctionId, true);

        Map<String, Object> status = new HashMap<>();
        status.put("auctionId", auctionId);
        status.put("itemName", auction.getItemName());
        status.put("active", auction.isActive());
        status.put("revealPhase", auction.isRevealPhase());
        status.put("reservePrice", auction.getReservePrice());
        status.put("totalBids", allBids.size());
        status.put("revealedBids", revealedBids.size());
        status.put("unrevealedBids", allBids.size() - revealedBids.size());
        status.put("nftTokenId", auction.getNftTokenId());

        if (!auction.isRevealPhase() && !auction.isActive() && auction.getWinnerAmount() != null) {
            status.put("resolved", true);
            status.put("winningAmount", auction.getWinnerAmount());
            status.put("winnerNullifier", auction.getWinnerNullifier());
        } else {
            status.put("resolved", false);
        }

        return status;
    }
}