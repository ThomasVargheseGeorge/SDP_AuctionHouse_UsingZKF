package com.auctionsdp.model;

import jakarta.persistence.*;


@Entity
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String itemName;
    private Double startingPrice;
    private Double currentHighestBid;
    private boolean active = true;

    
    private Double reservePrice = 0.0;
    private boolean revealPhase = false;

    private String winnerNullifier;
    private Double winnerAmount;

    private String nftTokenId;

    
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