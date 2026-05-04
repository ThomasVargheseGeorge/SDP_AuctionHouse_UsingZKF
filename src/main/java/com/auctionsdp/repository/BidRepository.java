package com.auctionsdp.repository;

import com.auctionsdp.model.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * BidRepository
 *
 * Stage 3 addition:
 * - findByNullifier — finds a specific bid during reveal phase
 */
public interface BidRepository extends JpaRepository<Bid, Long> {

    // Prevents double bidding
    boolean existsByNullifier(String nullifier);

    // Find specific bid by nullifier — used in reveal phase
    Optional<Bid> findByNullifier(String nullifier);

    // Get all bids for an auction
    List<Bid> findByAuctionId(Long auctionId);

    // Get revealed or unrevealed bids for an auction
    List<Bid> findByAuctionIdAndRevealed(Long auctionId, boolean revealed);
}