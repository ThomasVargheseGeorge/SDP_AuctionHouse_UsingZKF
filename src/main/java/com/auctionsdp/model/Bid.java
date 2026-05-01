package com.auctionsdp.model;

import jakarta.persistence.*;

@Entity
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long auctionId;

    private Double bidAmount;

    private String bidderId;

    // GETTERS & SETTERS

    public Long getId() {
        return id;
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }

    public Double getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(Double bidAmount) {
        this.bidAmount = bidAmount;
    }

    public String getBidderId() {
        return bidderId;
    }

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }
}