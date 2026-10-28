package me.iib.PintosLeiloes.models;

import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

/**
 * Written by iib <iib@realmclub.com>, 16:45 27/10/2026
 */
public class Input {
    public Type type;
    public List<ItemStack> items;
    public double price;
    public int auctionId;
    public ListState state;
    public BukkitTask timeout;
}