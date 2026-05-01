package com.auctionsdp.dto;

public class BidResponse {

    private Long auctionId;
    private Double bidAmount;
    private String anonymousBidder;

    public BidResponse(Long auctionId, Double bidAmount, String anonymousBidder) {
        this.auctionId = auctionId;
        this.bidAmount = bidAmount;
        this.anonymousBidder = anonymousBidder;
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public Double getBidAmount() {
        return bidAmount;
    }

    public String getAnonymousBidder() {
        return anonymousBidder;
    }
}