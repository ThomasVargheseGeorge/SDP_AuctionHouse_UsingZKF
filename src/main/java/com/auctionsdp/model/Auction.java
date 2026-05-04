package com.auctionsdp.model;

import jakarta.persistence.*;

/**
 * Auction
 *
 * Stage 3 additions:
 * - reservePrice    — minimum bid amount, public to all bidders
 * - revealPhase     — true when auction closes and reveal phase begins
 * - winnerNullifier — nullifier of the winning bid after reveal phase
 * - winnerAmount    — highest revealed bid amount after reveal phase
 * - nftTokenId      — the NFT being auctioned (Stage 5)
 */
@Entity
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String itemName;
    private Double startingPrice;
    private Double currentHighestBid;
    private boolean active = true;

    // Minimum valid bid — public to all bidders
    // Passed as reservePrice to the ZKP circuit
    private Double reservePrice = 0.0;

    // When true — auction is closed, bidders can now reveal amounts
    // When false — commit phase, bids are being accepted
    private boolean revealPhase = false;

    // Populated after reveal phase completes
    private String winnerNullifier;
    private Double winnerAmount;

    // NFT token ID being auctioned — Stage 5
    private String nftTokenId;

    // =============================
    // GETTERS & SETTERS
    // =============================

    public Long getId() { return id; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public Double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(Double startingPrice) { this.startingPrice = startingPrice; }

    public Double getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(Double currentHighestBid) { this.currentHighestBid = currentHighestBid; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Double getReservePrice() { return reservePrice; }
    public void setReservePrice(Double reservePrice) { this.reservePrice = reservePrice; }

    public boolean isRevealPhase() { return revealPhase; }
    public void setRevealPhase(boolean revealPhase) { this.revealPhase = revealPhase; }

    public String getWinnerNullifier() { return winnerNullifier; }
    public void setWinnerNullifier(String winnerNullifier) { this.winnerNullifier = winnerNullifier; }

    public Double getWinnerAmount() { return winnerAmount; }
    public void setWinnerAmount(Double winnerAmount) { this.winnerAmount = winnerAmount; }

    public String getNftTokenId() { return nftTokenId; }
    public void setNftTokenId(String nftTokenId) { this.nftTokenId = nftTokenId; }
}