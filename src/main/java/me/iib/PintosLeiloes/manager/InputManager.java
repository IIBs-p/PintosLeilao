package me.iib.PintosLeiloes.manager;

import me.iib.PintosLeiloes.PintosLeiloes;
import me.iib.PintosLeiloes.models.*;
import me.iib.PintosLeiloes.utils.Util;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Written by iib <iib@realmclub.com>, 16:41 27/10/2026
 */
public class InputManager implements Listener {


    private final Map<UUID, Input> inputs = new ConcurrentHashMap<>();

    public boolean has(Player p) {
        return inputs.containsKey(p.getUniqueId());
    }


    public void startSell(Player p, List<ItemStack> items) {
        Input in = new Input();
        in.type = Type.SELL_PRICE;
        in.items = items;
        register(p, in);
        PintosLeiloes.getInstance().getMessages().send(p, "sell.prompt-price", "player", p.getName(), "cancel", Util.cancelKeyword());
        PintosLeiloes.getInstance().getMessages().send(p, "sell.prompt-price-range", "player", p.getName(), "min", Util.money(Util.minPrice()), "max", Util.money(Util.maxPrice()));
    }

    public void startBid(Player p, Auction a, ListState back) {
        Input in = new Input();
        in.type = Type.BID;
        in.auctionId = a.id;
        in.state = back;
        register(p, in);
        PintosLeiloes.getInstance().getMessages().send(p, "bid.prompt", "player", p.getName(), "id", a.id, "item", Util.itemName(a.item), "seller", a.sellerName, "min", Util.money(PintosLeiloes.getInstance().getAuctionManager().minNextBid(a)), "cancel", Util.cancelKeyword());
    }

    public void startSearch(Player p, ListState s) {
        Input in = new Input();
        in.type = Type.SEARCH;
        in.state = s;
        register(p, in);
        PintosLeiloes.getInstance().getMessages().send(p, "input.search-prompt", "player", p.getName(), "cancel", Util.cancelKeyword());
    }

    private void register(Player p, Input in) {
        Input old = inputs.put(p.getUniqueId(), in);
        if (old != null) {
            if (old.timeout != null) {
                old.timeout.cancel();
            }
            returnItems(p, old);
        }
        arm(p, in);
    }

    private void arm(Player p, Input in) {
        if (in.timeout != null) {
            in.timeout.cancel();
        }
        in.timeout = Bukkit.getScheduler().runTaskLater(PintosLeiloes.getInstance(), () -> cancel(p, "input.timeout"), 20L * 60);
    }

    private void returnItems(Player p, Input in) {
        if (in.items == null) {
            return;
        }
        for (ItemStack it : in.items) {
            Util.giveOrDrop(p, it);
        }
        in.items = null;
    }

    public void cancel(Player p, String messageKey) {
        Input in = inputs.remove(p.getUniqueId());
        if (in == null) {
            return;
        }
        if (in.timeout != null) {
            in.timeout.cancel();
        }
        returnItems(p, in);
        if (messageKey != null && p.isOnline()) {
            PintosLeiloes.getInstance().getMessages().send(p, messageKey, "player", p.getName());
        }
    }

    public void shutdown() {
        for (UUID id : inputs.keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                cancel(p, "input.shutdown");
            }
        }
        inputs.clear();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (!inputs.containsKey(p.getUniqueId())) {
            return;
        }
        e.setCancelled(true);
        String msg = e.getMessage();
        Bukkit.getScheduler().runTask(PintosLeiloes.getInstance(), () -> handle(p, msg));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        PintosLeiloes.getInstance().getServer().getScheduler().runTaskLater(PintosLeiloes.getInstance(), () -> {
            if (e.getPlayer().isOnline()) {
                PintosLeiloes.getInstance().getAuctionManager().deliverPending(e.getPlayer());
            }
        }, 40L);
    }


    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        cancel(e.getPlayer(), null);
    }


    private void handle(Player p, String msg) {
        Input in = inputs.get(p.getUniqueId());
        if (in == null) {
            return;
        }
        msg = msg.trim();

        if (msg.equalsIgnoreCase(Util.cancelKeyword())) {
            cancel(p, "input.cancelled");
            return;
        }

        switch (in.type) {
            case SELL_PRICE: {
                Double v = Util.parseAmount(msg);
                if (v == null || v <= 0) {
                    PintosLeiloes.getInstance().getMessages().send(p, "sell.price-invalid", "player", p.getName());
                    arm(p, in);
                    return;
                }
                if (v < Util.minPrice() || v > Util.maxPrice()) {
                    PintosLeiloes.getInstance().getMessages().send(p, "sell.price-out-of-range", "player", p.getName(), "min", Util.money(Util.minPrice()), "max", Util.money(Util.maxPrice()));
                    arm(p, in);
                    return;
                }
                in.price = v;
                in.type = Type.SELL_DURATION;
                arm(p, in);
                PintosLeiloes.getInstance().getMessages().send(p, "sell.prompt-duration", "player", p.getName(), "min", Util.formatTime(Util.minDuration()), "max", Util.formatTime(Util.maxDuration()), "amount", Util.money(in.price), "cancel", Util.cancelKeyword());
                return;
            }
            case SELL_DURATION: {
                long ms = Util.parseDuration(msg);
                if (ms < 0) {
                    PintosLeiloes.getInstance().getMessages().send(p, "sell.duration-invalid", "player", p.getName());
                    arm(p, in);
                    return;
                }
                if (ms < Util.minDuration() || ms > Util.maxDuration()) {
                    PintosLeiloes.getInstance().getMessages().send(p, "sell.duration-out-of-range", "player", p.getName(), "min", Util.formatTime(Util.minDuration()), "max", Util.formatTime(Util.maxDuration()));
                    arm(p, in);
                    return;
                }
                inputs.remove(p.getUniqueId());
                if (in.timeout != null) in.timeout.cancel();
                boolean ok = PintosLeiloes.getInstance().getAuctionManager().createListings(p, in.items, in.price, ms);
                if (!ok) {
                    returnItems(p, in);
                } else {
                    in.items = null;
                    PintosLeiloes.getInstance().getMenuManager().openPlayer(p, p.getName());
                }
                return;
            }
            case BID: {
                Auction a = PintosLeiloes.getInstance().getAuctionManager().get(in.auctionId);
                if (a == null) {
                    cancel(p, "general.auction-ended");
                    return;
                }
                Double v = Util.parseAmount(msg);
                if (v == null || v <= 0) {
                    PintosLeiloes.getInstance().getMessages().send(p, "bid.invalid", "player", p.getName());
                    arm(p, in);
                    return;
                }
                BidResult r = PintosLeiloes.getInstance().getAuctionManager().placeBid(p, a, v);
                if (r == BidResult.TOO_LOW) {
                    arm(p, in);
                    return;
                }
                inputs.remove(p.getUniqueId());
                if (in.timeout != null) {
                    in.timeout.cancel();
                }
                if (PintosLeiloes.getInstance().getAuctionManager().get(a.id) != null) {
                    PintosLeiloes.getInstance().getMenuManager().openDetail(p, a, in.state);
                }
                return;
            }
            case SEARCH: {
                inputs.remove(p.getUniqueId());
                if (in.timeout != null) {
                    in.timeout.cancel();
                }
                in.state.search = msg;
                in.state.page = 0;
                PintosLeiloes.getInstance().getMenuManager().openList(p, in.state);
                return;
            }
            default:
        }
    }
}
