package com.auctionsdp.repository;

import com.auctionsdp.model.NFT;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NFTRepository extends JpaRepository<NFT, Long> {

    Optional<NFT> findByTokenId(String tokenId);
    List<NFT> findByOwnerNullifier(String ownerNullifier);
    List<NFT> findByAuctionId(Long auctionId);
    List<NFT> findByStatus(String status);
}