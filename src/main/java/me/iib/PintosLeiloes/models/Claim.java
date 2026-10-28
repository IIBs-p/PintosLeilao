package me.iib.PintosLeiloes.models;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Written by iib <iib@realmclub.com>, 16:00 27/10/2026
 */
public class Claim {
    public final int id;
    public final UUID owner;
    public final ItemStack item;
    public final String type;
    public final double price;
    public final long createdAt;

    public Claim(int id, UUID owner, ItemStack item, String type, double price, long createdAt) {
        this.id = id;
        this.owner = owner;
        this.item = item;
        this.type = type;
        this.price = price;
        this.createdAt = createdAt;
    }
}
