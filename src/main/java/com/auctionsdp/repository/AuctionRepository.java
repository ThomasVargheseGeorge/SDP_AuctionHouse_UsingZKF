package com.auctionsdp.repository;

import com.auctionsdp.model.Auction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
}