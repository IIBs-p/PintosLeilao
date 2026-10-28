package me.iib.PintosLeiloes.models;

import java.util.Comparator;

/**
 * Written by iib <iib@realmclub.com>, 15:54 27/10/2026
 */
public enum Sort {
    ENDING("Terminando primeiro", Comparator.comparingLong((Auction a) -> a.endTime)),
    NEWEST("Mais recentes", Comparator.comparingInt((Auction a) -> a.id).reversed()),
    PRICE_ASC("Menor preço", Comparator.comparingDouble(Auction::price)),
    PRICE_DESC("Maior preço", Comparator.comparingDouble(Auction::price).reversed());

    public final String label;
    public final Comparator<Auction> comparator;

    Sort(String label, Comparator<Auction> comparator) {
        this.label = label;
        this.comparator = comparator;
    }

    public Sort next() {
        Sort[] v = values();
        return v[(ordinal() + 1) % v.length];
    }
}