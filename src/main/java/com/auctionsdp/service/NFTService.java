package com.auctionsdp.service;

import com.auctionsdp.model.Auction;
import com.auctionsdp.model.NFT;
import com.auctionsdp.repository.AuctionRepository;
import com.auctionsdp.repository.NFTRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Service
public class NFTService {

    @Autowired
    private NFTRepository nftRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    
    public NFT mintNFT(String tokenId, String name, String description, String ownerNullifier) {
        // Check tokenId is unique
        if (nftRepository.findByTokenId(tokenId).isPresent()) {
            throw new RuntimeException("NFT with tokenId already exists: " + tokenId);
        }

        NFT nft = new NFT();
        nft.setTokenId(tokenId);
        nft.setName(name);
        nft.setDescription(description);
        nft.setOwnerNullifier(ownerNullifier);
        nft.setStatus("AVAILABLE");

        nftRepository.save(nft);

        System.out.println("[NFT] Minted: " + tokenId + " → owner: " + ownerNullifier);
        return nft;
    }

    public Map<String, Object> listNFTForAuction(String tokenId, Long auctionId) {
        NFT nft = nftRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new RuntimeException("NFT not found: " + tokenId));

        if (!nft.getStatus().equals("AVAILABLE")) {
            throw new RuntimeException("NFT is not available for listing. Status: " + nft.getStatus());
        }

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found: " + auctionId));

        nft.setAuctionId(auctionId);
        nft.setStatus("LISTED");
        nftRepository.save(nft);

        // Store tokenId on auction
        auction.setNftTokenId(tokenId);
        auctionRepository.save(auction);

        System.out.println("[NFT] Listed: " + tokenId + " in auction: " + auctionId);

        return Map.of(
                "message", "NFT listed for auction",
                "tokenId", tokenId,
                "auctionId", auctionId,
                "status", "LISTED"
        );
    }

    public Map<String, Object> transferNFTToWinner(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found: " + auctionId));
        if (auction.getWinnerNullifier() == null) {
            throw new RuntimeException("Auction not resolved yet — no winner determined");
        }

        if (auction.isActive() || auction.isRevealPhase()) {
            throw new RuntimeException("Auction must be fully resolved before NFT transfer");
        }

        
        String tokenId = auction.getNftTokenId();
        if (tokenId == null) {
            throw new RuntimeException("No NFT linked to this auction");
        }

        NFT nft = nftRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new RuntimeException("NFT not found: " + tokenId));

        String previousOwner = nft.getOwnerNullifier();
        String newOwner = auction.getWinnerNullifier();

      
        nft.setOwnerNullifier(newOwner);
        nft.setStatus("TRANSFERRED");
        nft.setAuctionId(null);
        nftRepository.save(nft);

        System.out.println("[NFT] Transferred: " + tokenId);
        System.out.println("[NFT] From: " + previousOwner);
        System.out.println("[NFT] To:   " + newOwner);

        return Map.of(
                "message", "NFT transferred to winner",
                "tokenId", tokenId,
                "nftName", nft.getName(),
                "previousOwner", previousOwner,
                "newOwner", newOwner,
                "winningAmount", auction.getWinnerAmount(),
                "auctionId", auctionId
        );
    }

    
    public NFT getNFT(String tokenId) {
        return nftRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new RuntimeException("NFT not found: " + tokenId));
    }

    
    public List<NFT> getNFTsByOwner(String ownerNullifier) {
        return nftRepository.findByOwnerNullifier(ownerNullifier);
    }

    
    public List<NFT> getAllNFTs() {
        return nftRepository.findAll();
    }

    
    public Map<String, Object> getAuctionNFTSummary(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found: " + auctionId));

        Map<String, Object> summary = new HashMap<>();
        summary.put("auctionId", auctionId);
        summary.put("itemName", auction.getItemName());
        summary.put("nftTokenId", auction.getNftTokenId());
        summary.put("winnerNullifier", auction.getWinnerNullifier());
        summary.put("winningAmount", auction.getWinnerAmount());
        summary.put("resolved", auction.getWinnerNullifier() != null);

        if (auction.getNftTokenId() != null) {
            nftRepository.findByTokenId(auction.getNftTokenId()).ifPresent(nft -> {
                summary.put("nftStatus", nft.getStatus());
                summary.put("currentOwner", nft.getOwnerNullifier());
            });
        }

        return summary;
    }
}