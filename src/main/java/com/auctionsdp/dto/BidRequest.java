package com.auctionsdp.dto;

public class BidRequest {
    public String userId;
    public String secret;
    public String auctionId;
    public String bidNonce;
    public Double bidAmount; // ⭐ important (actual bid)
}