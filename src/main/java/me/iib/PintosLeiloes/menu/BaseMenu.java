package me.iib.PintosLeiloes.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Written by iib <iib@realmclub.com>, 16:22 27/10/2026
 */
public abstract class BaseMenu implements InventoryHolder {
    public Inventory inv;

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
