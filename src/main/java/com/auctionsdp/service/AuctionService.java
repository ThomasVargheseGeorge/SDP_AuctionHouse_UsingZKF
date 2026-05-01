package com.auctionsdp.service;

import com.auctionsdp.model.Auction;
import com.auctionsdp.repository.AuctionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuctionService {

    @Autowired
    private AuctionRepository auctionRepository;

    public Auction createAuction(Auction auction) {
        auction.setCurrentHighestBid(auction.getStartingPrice());
        auction.setActive(true);
        return auctionRepository.save(auction);
    }

    public List<Auction> getAllAuctions() {
        return auctionRepository.findAll();
    }
}