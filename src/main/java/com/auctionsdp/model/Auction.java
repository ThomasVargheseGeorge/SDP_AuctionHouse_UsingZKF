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

    // GETTERS & SETTERS

    public Long getId() {
        return id;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(Double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public Double getCurrentHighestBid() {
        return currentHighestBid;
    }

    public void setCurrentHighestBid(Double currentHighestBid) {
        this.currentHighestBid = currentHighestBid;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}