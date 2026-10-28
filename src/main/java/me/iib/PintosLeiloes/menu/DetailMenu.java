package me.iib.PintosLeiloes.menu;

import me.iib.PintosLeiloes.models.ListState;

/**
 * Written by iib <iib@realmclub.com>, 16:23 27/10/2026
 */
public class DetailMenu extends BaseMenu {
    public final int auctionId;
    public final ListState back;

    public DetailMenu(int auctionId, ListState back) {
        this.auctionId = auctionId;
        this.back = back;
    }
}