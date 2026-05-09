package com.auctionsdp.model;

import jakarta.persistence.*;


@Entity
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long auctionId;

    
    private String bidCommitment;

    @Column(unique = true)
    private String nullifier;

    private boolean revealed = false;
    private Double revealedAmount;
    private String bidderId = "ZKP_VERIFIED";

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