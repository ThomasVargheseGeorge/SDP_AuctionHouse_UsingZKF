package com.auctionsdp.repository;

import com.auctionsdp.model.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * BidRepository
 *
 * Stage 1 additions:
 * - existsByNullifier        — checks if a nullifier has already been used
 * - findByAuctionId          — gets all bids for a specific auction
 * - findByAuctionIdAndRevealed — gets revealed or unrevealed bids for an auction
 */
public interface BidRepository extends JpaRepository<Bid, Long> {

    // Prevents double bidding — called before saving any new bid
    boolean existsByNullifier(String nullifier);

    // Get all bids for a specific auction
    List<Bid> findByAuctionId(Long auctionId);

    // Get only revealed or only unrevealed bids for an auction
    // Used during reveal phase to find who has revealed so far
    List<Bid> findByAuctionIdAndRevealed(Long auctionId, boolean revealed);
}