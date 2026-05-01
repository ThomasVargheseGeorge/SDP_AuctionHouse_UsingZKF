package com.auctionsdp.repository;
import com.auctionsdp.model.Bid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidRepository extends JpaRepository<Bid, Long> {
}