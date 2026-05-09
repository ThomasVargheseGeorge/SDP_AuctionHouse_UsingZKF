package com.auctionsdp.model;
import jakarta.persistence.*;


@Entity
public class NFT {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tokenId;

    private String name;

    private String description;

    private String ownerNullifier;
    private Long auctionId;

    private String status = "AVAILABLE";


    public Long getId() { return id; }

    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOwnerNullifier() { return ownerNullifier; }
    public void setOwnerNullifier(String ownerNullifier) { this.ownerNullifier = ownerNullifier; }

    public Long getAuctionId() { return auctionId; }
    public void setAuctionId(Long auctionId) { this.auctionId = auctionId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}