package me.iib.PintosLeiloes.models;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Written by iib <iib@realmclub.com>, 16:18 27/10/2026
 */
public class Auction {
    public final int id;
    public final UUID seller;
    public final String sellerName;
    public final ItemStack item;
    public final double startPrice;
    public double currentBid;
    public UUID bidder;
    public String bidderName;
    public final long createdAt;
    public long endTime;

    public Auction(int id, UUID seller, String sellerName, ItemStack item, double startPrice, double currentBid, UUID bidder, String bidderName, long createdAt, long endTime) {
        this.id = id;
        this.seller = seller;
        this.sellerName = sellerName;
        this.item = item;
        this.startPrice = startPrice;
        this.currentBid = currentBid;
        this.bidder = bidder;
        this.bidderName = bidderName;
        this.createdAt = createdAt;
        this.endTime = endTime;
    }

    public boolean hasBids() {
        return bidder != null;
    }

    public double price() {
        return currentBid;
    }
}
