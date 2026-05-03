package com.auctionsdp.model;

import jakarta.persistence.*;

/**
 * Bid
 *
 * Stage 1 changes:
 * - bidAmount (Double) removed — amount is now hidden until reveal phase
 * - bidCommitment added — stores Poseidon(bidAmount, bidderSecret)
 * - nullifier added — prevents double bidding
 * - revealed added — tracks whether bidder has revealed their amount yet
 * - revealedAmount added — populated only after reveal phase
 */
@Entity
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long auctionId;

    // Hidden bid — Poseidon(bidAmount, bidderSecret)
    // Plain amount is never stored here
    private String bidCommitment;

    // Secure Poseidon nullifier — prevents the same proof being used twice
    @Column(unique = true)
    private String nullifier;

    // Has the bidder revealed their amount yet?
    private boolean revealed = false;

    // Populated during reveal phase — null until then
    private Double revealedAmount;

    // Bidder identity is always anonymous
    private String bidderId = "ZKP_VERIFIED";

    // =============================
    // GETTERS & SETTERS
    // =============================

    public Long getId() {
        return id;
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }

    public String getBidCommitment() {
        return bidCommitment;
    }

    public void setBidCommitment(String bidCommitment) {
        this.bidCommitment = bidCommitment;
    }

    public String getNullifier() {
        return nullifier;
    }

    public void setNullifier(String nullifier) {
        this.nullifier = nullifier;
    }

    public boolean isRevealed() {
        return revealed;
    }

    public void setRevealed(boolean revealed) {
        this.revealed = revealed;
    }

    public Double getRevealedAmount() {
        return revealedAmount;
    }

    public void setRevealedAmount(Double revealedAmount) {
        this.revealedAmount = revealedAmount;
    }

    public String getBidderId() {
        return bidderId;
    }

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }
}