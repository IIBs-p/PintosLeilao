package me.iib.PintosLeiloes.manager;

import me.iib.PintosLeiloes.PintosLeiloes;
import me.iib.PintosLeiloes.models.Auction;
import me.iib.PintosLeiloes.models.BidResult;
import me.iib.PintosLeiloes.models.Claim;
import me.iib.PintosLeiloes.utils.Util;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Written by iib <iib@realmclub.com>, 16:02 27/10/2026
 */
public class AuctionManager {

    private final Map<Integer, Auction> active = new LinkedHashMap<>();

    private Object[] base(Auction a) {
        return new Object[]{
                "id", a.id,
                "item", Util.itemName(a.item),
                "seller", a.sellerName,
                "bidder", a.bidderName == null ? "-" : a.bidderName,
                "amount", Util.money(a.currentBid),
                "time", Util.formatTime(a.endTime - System.currentTimeMillis())
        };
    }

    private static Object[] cat(Object[] a, Object... b) {
        Object[] r = new Object[a.length + b.length];

        System.arraycopy(b, 0, r, 0, b.length);
        System.arraycopy(a, 0, r, b.length, a.length);
        return r;
    }

    public void loadActive() {
        for (Auction a : PintosLeiloes.getInstance().getDatabaseManager().loadActive()) {
            active.put(a.id, a);
        }
    }

    public List<Auction> activeList() {
        return new ArrayList<>(active.values());
    }

    public Auction get(int id) {
        return active.get(id);
    }

    public int countBySeller(UUID uuid) {
        int n = 0;

        for (Auction a : active.values()) {
            if (a.seller.equals(uuid)) {
                n++;
            }
        }
        return n;
    }

    public Auction findFirstBySellerName(String name) {
        for (Auction a : active.values()) {
            if (a.sellerName.equalsIgnoreCase(name)) {
                return a;
            }
        }
        return null;
    }

    public List<String> sellerNames() {
        List<String> names = new ArrayList<>();
        for (Auction a : active.values()) {
            if (!names.contains(a.sellerName)) {
                names.add(a.sellerName);
            }
        }
        return names;
    }

    public double minNextBid(Auction a) {
        return a.hasBids() ? a.currentBid + Util.minIncrement() : a.startPrice;
    }


    public boolean createListings(Player p, List<ItemStack> items, double price, long duration) {
        if (!p.hasPermission("leilao.bypass.limit") && countBySeller(p.getUniqueId()) + items.size() > Util.maxItems()) {
            PintosLeiloes.getInstance().getMessages().send(p, "sell.limit-exceeded", "player", p.getName(), "max", Util.maxItems());
            return false;
        }
        for (ItemStack i : items) {
            if (Util.isBanned(i)) {
                PintosLeiloes.getInstance().getMessages().send(p, "sell.banned-item", "player", p.getName(), "item", Util.itemName(i));
                return false;
            }
        }
        long now = System.currentTimeMillis();
        int created = 0;
        for (ItemStack i : items) {
            int id = PintosLeiloes.getInstance().getDatabaseManager().insertAuction(p.getUniqueId(), p.getName(), i, price, now, now + duration);
            if (id < 0) {
                Util.giveOrDrop(p, i);
                continue;
            }
            Auction a = new Auction(id, p.getUniqueId(), p.getName(), i.clone(), price, price, null, null, now, now + duration);
            active.put(id, a);
            created++;
        }
        PintosLeiloes.getInstance().getMessages().send(p, "sell.created", "player", p.getName(), "count", created, "time", Util.formatTime(duration), "amount", Util.money(price));
        return true;
    }

    public BidResult placeBid(Player p, Auction a, double amount) {

        if (PintosLeiloes.getInstance().getEconomy() == null) {
            PintosLeiloes.getInstance().getMessages().send(p, "general.economy-unavailable", "player", p.getName());
            return BidResult.FAILED;
        }
        if (!active.containsKey(a.id) || a.endTime <= System.currentTimeMillis()) {
            PintosLeiloes.getInstance().getMessages().send(p, "general.auction-ended", "player", p.getName());
            return BidResult.FAILED;
        }
        if (a.seller.equals(p.getUniqueId())) {
            PintosLeiloes.getInstance().getMessages().send(p, "bid.own-item", cat(base(a), "player", p.getName()));
            return BidResult.FAILED;
        }
        if (p.getUniqueId().equals(a.bidder)) {
            PintosLeiloes.getInstance().getMessages().send(p, "bid.already-top", cat(base(a), "player", p.getName()));
            return BidResult.FAILED;
        }
        double min = minNextBid(a);
        if (amount + 0.0001 < min) {
            PintosLeiloes.getInstance().getMessages().send(p, "bid.too-low", cat(base(a), "player", p.getName(), "min", Util.money(min), "cancel", Util.cancelKeyword()));
            return BidResult.TOO_LOW;
        }
        if (!PintosLeiloes.getInstance().getEconomy().has(p, amount)) {
            PintosLeiloes.getInstance().getMessages().send(p, "bid.no-funds", cat(base(a), "player", p.getName(), "amount", Util.money(amount)));
            return BidResult.NO_FUNDS;
        }

        EconomyResponse r = PintosLeiloes.getInstance().getEconomy().withdrawPlayer(p, amount);
        if (!r.transactionSuccess()) {
            PintosLeiloes.getInstance().getMessages().send(p, "bid.withdraw-failed", cat(base(a), "player", p.getName(), "error", r.errorMessage));
            return BidResult.FAILED;
        }

        String oldAmount = Util.money(a.currentBid);
        UUID previous = a.bidder;

        if (previous != null) {
            PintosLeiloes.getInstance().getEconomy().depositPlayer(Bukkit.getOfflinePlayer(previous), a.currentBid);
            notify(previous, "bid.notify-outbid", cat(base(a), "player", p.getName(), "old_amount", oldAmount, "amount", Util.money(amount)));
        }

        a.currentBid = amount;
        a.bidder = p.getUniqueId();
        a.bidderName = p.getName();

        int snipe = Util.antiSnipeSeconds();
        if (snipe > 0 && a.endTime - System.currentTimeMillis() < snipe * 1000L) {
            a.endTime += snipe * 1000L;
        }
        PintosLeiloes.getInstance().getDatabaseManager().updateBid(a.id, a.currentBid, a.bidder, a.bidderName, a.endTime);

        PintosLeiloes.getInstance().getMessages().send(p, "bid.placed", cat(base(a), "player", p.getName()));
        notify(a.seller, "bid.notify-seller", cat(base(a), "player", p.getName()));

        if (amount >= Util.broadcastThreshold()) {
            broadcastHighBid(p, a);
        }
        return BidResult.OK;
    }

    private void broadcastHighBid(Player bidder, Auction a) {
        Object[] ph = cat(base(a), "player", bidder.getName());
        TextComponent msg = new TextComponent(PintosLeiloes.getInstance().getMessages().get("bid.broadcast", ph));
        TextComponent click = new TextComponent(PintosLeiloes.getInstance().getMessages().get("bid.broadcast-button", ph));
        click.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/leilao view " + a.id));
        click.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(PintosLeiloes.getInstance().getMessages().get("bid.broadcast-hover", ph))));
        msg.addExtra(click);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.spigot().sendMessage(msg);
        }
    }


    public void tick() {
        if (PintosLeiloes.getInstance().getEconomy() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Auction a : new ArrayList<>(active.values())) {
            if (a.endTime <= now) {
                finish(a);
            }
        }
    }

    public void finish(Auction a) {
        if (PintosLeiloes.getInstance().getEconomy() == null) {
            return;
        }
        active.remove(a.id);
        PintosLeiloes.getInstance().getDatabaseManager().setStatus(a.id, "ENDED");

        if (a.hasBids()) {
            double tax = a.currentBid * Util.taxPercent() / 100.0;
            double net = a.currentBid - tax;
            PintosLeiloes.getInstance().getEconomy().depositPlayer(Bukkit.getOfflinePlayer(a.seller), net);
            PintosLeiloes.getInstance().getDatabaseManager().insertClaim(a.bidder, a.item, "WON", a.currentBid, a.id);
            notify(a.bidder, "end.won", cat(base(a), "player", a.bidderName));
            String taxText = tax > 0 ? PintosLeiloes.getInstance().getMessages().get("end.tax-suffix", "tax", Util.money(tax)) : "";
            notify(a.seller, "end.sold", cat(base(a), "player", a.sellerName, "tax", taxText));
        } else {
            PintosLeiloes.getInstance().getDatabaseManager().insertClaim(a.seller, a.item, "RETURNED", 0, a.id);
            notify(a.seller, "end.no-bids", cat(base(a), "player", a.sellerName));
        }
    }

    public void cancel(Auction a, Player by) {
        if (!active.containsKey(a.id)) {
            return;
        }

        active.remove(a.id);
        PintosLeiloes.getInstance().getDatabaseManager().setStatus(a.id, "CANCELLED");
        if (a.hasBids()) {
            if (PintosLeiloes.getInstance().getEconomy() != null) {
                PintosLeiloes.getInstance().getEconomy().depositPlayer(Bukkit.getOfflinePlayer(a.bidder), a.currentBid);
            }
            notify(a.bidder, "cancel.bidder-refund", cat(base(a), "player", a.bidderName));
        }
        PintosLeiloes.getInstance().getDatabaseManager().insertClaim(a.seller, a.item, "RETURNED", 0, a.id);

        if (by == null || !by.getUniqueId().equals(a.seller)) {
            notify(a.seller, "cancel.seller-notified", cat(base(a), "player", a.sellerName));
        } else {
            PintosLeiloes.getInstance().getMessages().send(by, "cancel.self", cat(base(a), "player", by.getName()));
        }
    }

    public List<Claim> claims(UUID uuid) {
        return PintosLeiloes.getInstance().getDatabaseManager().getClaims(uuid);
    }

    public boolean claimItem(Player p, int claimId) {
        Claim c = PintosLeiloes.getInstance().getDatabaseManager().getClaim(claimId, p.getUniqueId());
        if (c == null) {
            return false;
        }
        if (!Util.hasSpace(p, c.item)) {
            PintosLeiloes.getInstance().getMessages().send(p, "general.inventory-full", "player", p.getName());
            return false;
        }
        PintosLeiloes.getInstance().getDatabaseManager().deleteClaim(c.id);
        p.getInventory().addItem(c.item.clone());
        PintosLeiloes.getInstance().getMessages().send(p, "general.item-claimed", "player", p.getName(), "item", Util.itemName(c.item));
        return true;
    }

    public int claimCount(UUID uuid) {
        return PintosLeiloes.getInstance().getDatabaseManager().countClaims(uuid);
    }

    public void notify(UUID uuid, String key, Object... ph) {
        String text = PintosLeiloes.getInstance().getMessages().get(key, ph);
        if (text.isEmpty()) {
            return;
        }
        Player p = Bukkit.getPlayer(uuid);

        if (p != null && p.isOnline()) {
            p.sendMessage(text);
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        } else {
            PintosLeiloes.getInstance().getDatabaseManager().insertNotification(uuid, text);
        }
    }

    public void deliverPending(Player p) {
        for (String text : PintosLeiloes.getInstance().getDatabaseManager().popNotifications(p.getUniqueId())) {
            p.sendMessage(text);
        }
        int n = PintosLeiloes.getInstance().getDatabaseManager().countClaims(p.getUniqueId());
        if (n > 0) {
            PintosLeiloes.getInstance().getMessages().send(p, "general.pending-claims", "player", p.getName(), "count", n);
        }
    }
}
