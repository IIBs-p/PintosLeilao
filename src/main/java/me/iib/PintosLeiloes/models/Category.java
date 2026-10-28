package me.iib.PintosLeiloes.models;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Written by iib <iib@realmclub.com>, 16:01 27/10/2026
 */
public enum Category {
    ALL("Todos os itens", Material.DIAMOND_SWORD),
    EQUIP("Equipamentos", Material.DIAMOND_CHESTPLATE),
    BLOCKS("Blocos", Material.STONE),
    FOOD("Comidas", Material.APPLE),
    OTHER("Outros", Material.EMERALD);

    public final String label;
    public final Material icon;

    Category(String label, Material icon) {
        this.label = label;
        this.icon = icon;
    }

    public boolean matches(ItemStack item) {
        return this == ALL || of(item) == this;
    }

    public static Category of(ItemStack item) {
        Material m = item.getType();
        String n = m.name();
        if (n.endsWith("_SWORD") || n.endsWith("_AXE") || n.endsWith("_PICKAXE") || n.endsWith("_SHOVEL") || n.endsWith("_HOE") || n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS") || n.equals("BOW") || n.equals("CROSSBOW") || n.equals("TRIDENT") || n.equals("SHIELD") || n.equals("ELYTRA") || n.equals("FISHING_ROD") || n.equals("MACE") || n.equals("TURTLE_HELMET")) {
            return EQUIP;
        }

        if (m.isEdible()) {
            return FOOD;
        }

        if (m.isBlock()) {
            return BLOCKS;
        }

        return OTHER;
    }
}
