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

/**
 * RevealService
 *
 * Handles the reveal phase of the sealed-bid auction.
 *
 * Flow:
 * 1. Auctioneer closes the auction — sets revealPhase = true
 * 2. Each bidder calls /reveal with their nullifier, bidAmount, bidderSecret
 * 3. Server verifies Poseidon(bidAmount, bidderSecret) === stored bidCommitment
 * 4. If valid, stores revealedAmount on the bid
 * 5. After all reveals, /resolve-auction finds highest revealed bid
 * 6. Winner's nullifier and amount stored on auction
 */
@Service
public class RevealService {

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    // =============================
    // CLOSE AUCTION — start reveal phase
    // Called by auctioneer when bidding window ends
    // =============================
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

    // =============================
    // REVEAL BID
    // Bidder reveals their actual bid amount
    // Server verifies commitment: Poseidon(bidAmount, bidderSecret) === bidCommitment
    // =============================
    public Map<String, Object> revealBid(
            String nullifier,
            BigInteger bidAmount,
            BigInteger bidderSecret
    ) {
        // Find the bid by nullifier
        Optional<Bid> bidOpt = bidRepository.findByNullifier(nullifier);
        if (bidOpt.isEmpty()) {
            throw new RuntimeException("Bid not found for nullifier: " + nullifier);
        }

        Bid bid = bidOpt.get();

        // Check not already revealed
        if (bid.isRevealed()) {
            throw new RuntimeException("Bid already revealed");
        }

        // Check auction is in reveal phase
        Optional<Auction> auctionOpt = auctionRepository.findById(bid.getAuctionId());
        if (auctionOpt.isEmpty()) {
            throw new RuntimeException("Auction not found");
        }

        Auction auction = auctionOpt.get();
        if (!auction.isRevealPhase()) {
            throw new RuntimeException("Auction is not in reveal phase yet");
        }

        // Verify commitment: Poseidon(bidAmount, bidderSecret) === stored bidCommitment
        BigInteger computedCommitment = PoseidonHash.hash(bidAmount, bidderSecret);
        String storedCommitment = bid.getBidCommitment();

        if (!computedCommitment.toString().equals(storedCommitment)) {
            throw new RuntimeException(
                "Commitment mismatch — revealed amount does not match committed value"
            );
        }

        // Valid reveal — store the amount
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

    // =============================
    // RESOLVE AUCTION
    // Finds highest revealed bid and declares winner
    // Called after reveal phase — all bidders should have revealed
    // =============================
    public Map<String, Object> resolveAuction(Long auctionId) {
        Optional<Auction> auctionOpt = auctionRepository.findById(auctionId);
        if (auctionOpt.isEmpty()) {
            throw new RuntimeException("Auction not found: " + auctionId);
        }

        Auction auction = auctionOpt.get();
        if (!auction.isRevealPhase()) {
            throw new RuntimeException("Auction must be in reveal phase to resolve");
        }

        // Get all revealed bids for this auction
        List<Bid> revealedBids = bidRepository.findByAuctionIdAndRevealed(auctionId, true);

        if (revealedBids.isEmpty()) {
            throw new RuntimeException("No revealed bids found for auction: " + auctionId);
        }

        // Find highest revealed bid
        Bid winningBid = revealedBids.get(0);
        for (Bid bid : revealedBids) {
            if (bid.getRevealedAmount() > winningBid.getRevealedAmount()) {
                winningBid = bid;
            }
        }

        // Store winner on auction
        auction.setWinnerNullifier(winningBid.getNullifier());
        auction.setWinnerAmount(winningBid.getRevealedAmount());
        auction.setCurrentHighestBid(winningBid.getRevealedAmount());
        auction.setRevealPhase(false);
        auctionRepository.save(auction);

        return Map.of(
                "message", "Auction resolved — winner determined",
                "auctionId", auctionId,
                "winnerNullifier", winningBid.getNullifier(),
                "winningAmount", winningBid.getRevealedAmount(),
                "totalRevealed", revealedBids.size()
        );
    }

    // =============================
    // GET AUCTION STATUS
    // Returns current state without revealing individual bid amounts
    // =============================
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

        // Only show winner if auction is resolved
        if (!auction.isRevealPhase() && !auction.isActive() && auction.getWinnerAmount() != null) {
            status.put("resolved", true);
            status.put("winningAmount", auction.getWinnerAmount());
        } else {
            status.put("resolved", false);
        }

        return status;
    }
}