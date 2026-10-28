package me.iib.PintosLeiloes.utils;

import me.iib.PintosLeiloes.PintosLeiloes;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Written by iib <iib@realmclub.com>, 15:35 27/10/2026
 */
public class Util {

    public static ItemStack icon(Material mat, String name, String... lore) {
        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta();

        if (m != null) {
            m.setDisplayName(name);

            if (lore.length > 0) {
                m.setLore(Arrays.asList(lore));
            }

            m.addItemFlags(ItemFlag.values());
            i.setItemMeta(m);
        }
        return i;
    }

    private static String tDays = "d";
    private static String tHours = "h";
    private static String tMinutes = "m";
    private static String tSeconds = "s";
    private static String tEnded = "encerrando...";
    private static String amountFormat = " §7x{amount}";

    public static void setTimeFormat(String d, String h, String m, String s, String ended, String amount) {
        tDays = d;
        tHours = h;
        tMinutes = m;
        tSeconds = s;
        tEnded = ended;
        amountFormat = amount;
    }

    public static String formatTime(long ms) {
        if (ms <= 0) {
            return tEnded;
        }

        long s = ms / 1000;
        long d = s / 86400;
        long h = (s % 86400) / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) {
            sb.append(d).append(tDays).append(" ");
        }

        if (h > 0) {
            sb.append(h).append(tHours).append(" ");
        }

        if (m > 0) {
            sb.append(m).append(tMinutes).append(" ");
        }

        if (d == 0 && h == 0) {
            sb.append(sec).append(tSeconds);
        }

        return sb.toString().trim();
    }

    public static long parseDuration(String in) {
        String s = in.toLowerCase().replace(" ", "");
        if (!s.matches("(\\d+[smhd])+")) {
            return -1;
        }
        try {
            Matcher m = Pattern.compile("(\\d+)([smhd])").matcher(s);
            long total = 0;
            while (m.find()) {
                long n = Long.parseLong(m.group(1));
                switch (m.group(2).charAt(0)) {
                    case 's': total += n * 1000L; break;
                    case 'm': total += n * 60_000L; break;
                    case 'h': total += n * 3_600_000L; break;
                    default: total += n * 86_400_000L; break;
                }
            }
            return total;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static Double parseAmount(String in) {
        String s = in.trim().toLowerCase().replace(",", ".").replace("$", "");
        if (s.isEmpty()) {
            return null;
        }

        double mult = 1;
        char last = s.charAt(s.length() - 1);
        if (last == 'k') {
            mult = 1_000d;
        } else if (last == 'm') {
            mult = 1_000_000d;
        } else if (last == 'b') {
            mult = 1_000_000_000d;
        }

        if (mult != 1) {
            s = s.substring(0, s.length() - 1);
        }

        try {
            double v = Double.parseDouble(s) * mult;
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                return null;
            }
            return Math.round(v * 100d) / 100d;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String prettyMaterial(Material m) {
        String[] parts = m.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    public static String itemName(ItemStack i) {
        ItemMeta m = i.getItemMeta();
        String n = (m != null && m.hasDisplayName()) ? m.getDisplayName() : prettyMaterial(i.getType());
        return n + (i.getAmount() > 1 ? amountFormat.replace("{amount}", String.valueOf(i.getAmount())) : "");
    }

    public static boolean hasSpace(Player p, ItemStack item) {
        int room = 0;
        ItemStack[] contents = p.getInventory().getStorageContents();
        for (ItemStack c : contents) {
            if (c == null || c.getType() == Material.AIR) {
                room += item.getMaxStackSize();
            } else if (c.isSimilar(item)) {
                room += Math.max(0, c.getMaxStackSize() - c.getAmount());
            }
            if (room >= item.getAmount()) {
                return true;
            }
        }
        return false;
    }

    public static void giveOrDrop(Player p, ItemStack item) {
        PlayerInventory inv = p.getInventory();
        Map<Integer, ItemStack> left = inv.addItem(item);
        for (ItemStack l : left.values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), l);
        }
    }
    public static String toBase64(ItemStack item) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             BukkitObjectOutputStream out = new BukkitObjectOutputStream(bos)) {
            out.writeObject(item);
            out.flush();
            return Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException(" ", e);
        }
    }

    public static ItemStack fromBase64(String data) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(Base64.getDecoder().decode(data));
             BukkitObjectInputStream in = new BukkitObjectInputStream(bis)) {
            return (ItemStack) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException(" ", e);
        }
    }

    public static boolean isBanned(ItemStack i) {
        return i == null || i.getType() == Material.AIR || PintosLeiloes.getInstance().getBanned().contains(i.getType());
    }

    public static int maxItems() {
        return PintosLeiloes.getInstance().getConfig().getInt("max-items-per-player", 5);
    }

    public static double minPrice() {
        return PintosLeiloes.getInstance().getConfig().getDouble("min-start-price", 1.0);
    }

    public static double maxPrice() {
        return PintosLeiloes.getInstance().getConfig().getDouble("max-start-price", 1_000_000_000d);
    }

    public static double minIncrement() {
        return PintosLeiloes.getInstance().getConfig().getDouble("min-bid-increment", 1.0);
    }

    public static double broadcastThreshold() {
        return PintosLeiloes.getInstance().getConfig().getDouble("broadcast-bid-threshold", 10000.0);
    }

    public static double taxPercent() {
        return PintosLeiloes.getInstance().getConfig().getDouble("tax-percent", 0.0);
    }

    public static int antiSnipeSeconds() {
        return PintosLeiloes.getInstance().getConfig().getInt("anti-snipe-seconds", 0);
    }

    public static long minDuration() {
        long v = parseDuration(PintosLeiloes.getInstance().getConfig().getString("min-duration", "1m"));
        return v > 0 ? v : 60_000L;
    }

    public static long maxDuration() {
        long v = parseDuration(PintosLeiloes.getInstance().getConfig().getString("max-duration", "7d"));
        return v > 0 ? v : 7 * 86_400_000L;
    }

    public static String cancelKeyword() {
        return PintosLeiloes.getInstance().getConfig().getString("cancel-keyword", "cancelar");
    }

    public static String money(double v) {
        Economy e = PintosLeiloes.getInstance().getEconomy();
        return e != null ? e.format(v) : String.format(Locale.US, "%.2f", v);
    }

}
