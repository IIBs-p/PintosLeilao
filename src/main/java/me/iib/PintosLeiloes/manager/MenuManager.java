package me.iib.PintosLeiloes.manager;

import com.google.common.base.Utf8;
import me.iib.PintosLeiloes.PintosLeiloes;
import me.iib.PintosLeiloes.menu.*;
import me.iib.PintosLeiloes.models.*;
import me.iib.PintosLeiloes.utils.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Written by iib <iib@realmclub.com>, 16:21 27/10/2026
 */
public class MenuManager implements Listener {

    private static final int PER_PAGE = 24;
    private static final int[] ITEM_SLOTS = new int[PER_PAGE];
    private static final int[] CATEGORY_SLOTS = {0, 9, 18, 27, 36};
    private static final int SLOT_PREV = 45;
    private static final int SLOT_BACK = 46;
    private static final int SLOT_SEARCH = 47;
    private static final int SLOT_SORT = 48;
    private static final int SLOT_REFRESH = 49;
    private static final int SLOT_SELL = 50;
    private static final int SLOT_WIN = 51;
    private static final int SLOT_MINE = 52;
    private static final int SLOT_NEXT = 53;

    private static final int SELL_CANCEL = 29;
    private static final int SELL_INFO = 31;
    private static final int SELL_CONFIRM = 33;
    private static final int D_BACK = 11;
    private static final int D_ITEM = 13;
    private static final int D_ACTION = 15;
    private static final int D_ADMIN = 17;
    private static final int D_SELLER = 22;

    static {
        int i = 0;
        for (int r = 1; r <= 4; r++) {
            for (int c = 2; c <= 7; c++) {
                ITEM_SLOTS[i++] = r * 9 + c;
            }
        }
    }


    private void later(Runnable r) {
        Bukkit.getScheduler().runTask(PintosLeiloes.getInstance(), r);
    }

    private String title(String s) {
        return s.length() > 32 ? s.substring(0, 32) : s;
    }

    private ItemStack pane() {
        return Util.icon(Material.BLUE_STAINED_GLASS_PANE, " ");
    }

    private void fill(Inventory inv, ItemStack it) {
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, it);
        }
    }

    private ItemStack btn(Material mat, String key, Object... ph) {
        List<String> lore = PintosLeiloes.getInstance().getMessages().list("gui.buttons." + key + ".lore", ph);
        return Util.icon(mat, PintosLeiloes.getInstance().getMessages().get("gui.buttons." + key + ".name", ph), lore.toArray(new String[0]));
    }

    private void addLore(ItemStack it, List<String> extra) {
        ItemMeta m = it.getItemMeta();
        if (m == null) {
            return;
        }

        List<String> lore = new ArrayList<>();
        if (m.hasLore() && m.getLore() != null) {
            lore.addAll(m.getLore());
        }

        lore.addAll(extra);
        m.setLore(lore);
        it.setItemMeta(m);
    }

    private ItemStack auctionIcon(Auction a, boolean detail) {
        ItemStack it = a.item.clone();
        Object[] ph = {
                "seller", a.sellerName,
                "bidder", a.bidderName == null ? "-" : a.bidderName,
                "amount", a.hasBids() ? Util.money(a.currentBid) : Util.money(a.startPrice),
                "time", Util.formatTime(a.endTime - System.currentTimeMillis()),
                "id", a.id
        };
        List<String> extra = new ArrayList<>(PintosLeiloes.getInstance().getMessages().list(a.hasBids() ? "gui.auction-lore.with-bids" : "gui.auction-lore.no-bids", ph));
        if (!detail) {
            extra.addAll(PintosLeiloes.getInstance().getMessages().list("gui.auction-lore.click", ph));
        }
        addLore(it, extra);
        return it;
    }

    private ItemStack claimIcon(Claim c) {
        ItemStack it = c.item.clone();
        addLore(it, PintosLeiloes.getInstance().getMessages().list("WON".equals(c.type) ? "gui.claim-lore.won" : "gui.claim-lore.returned", "amount", Util.money(c.price)));
        return it;
    }

    private boolean matchesSearch(Auction a, String q) {
        if (q == null || q.isEmpty()) {
            return true;
        }

        q = q.toLowerCase();
        if (a.sellerName.toLowerCase().contains(q)) {
            return true;
        }
        if (a.item.getType().name().toLowerCase().replace('_', ' ').contains(q)) {
            return true;
        }
        ItemMeta m = a.item.getItemMeta();
        if (m != null && m.hasDisplayName()) {
            String dn = org.bukkit.ChatColor.stripColor(m.getDisplayName());
            return dn != null && dn.toLowerCase().contains(q);
        }
        return false;
    }

    public void openMarket(Player p) {
        openList(p, new ListState());
    }

    public void openPlayer(Player p, String name) {
        Auction any = PintosLeiloes.getInstance().getAuctionManager().findFirstBySellerName(name);
        if (any == null) {
            openEmpty(p, name);
            return;
        }
        ListState s = new ListState();
        s.mode = Mode.PLAYER;
        s.target = any.seller;
        s.targetName = any.sellerName;
        openList(p, s);
    }

    public void openWin(Player p) {
        ListState s = new ListState();
        s.mode = Mode.WIN;
        openList(p, s);
    }

    public void openEmpty(Player p, String name) {
        EmptyMenu h = new EmptyMenu();
        Inventory inv = Bukkit.createInventory(h, 27, title(PintosLeiloes.getInstance().getMessages().get("gui.titles.player", "player", name)));
        h.inv = inv;
        ItemStack blank = Util.icon(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 9; i < 18; i++) {
            inv.setItem(i, blank);
        }
        p.openInventory(inv);
    }

    public void openList(Player p, ListState s) {
        if (s.mode == Mode.WIN) {
            openClaims(p, s);
            return;
        }
        long now = System.currentTimeMillis();
        List<Auction> list = PintosLeiloes.getInstance().getAuctionManager().activeList().stream()
                .filter(a -> a.endTime > now)
                .filter(a -> s.mode != Mode.PLAYER || a.seller.equals(s.target))
                .filter(a -> s.category.matches(a.item))
                .filter(a -> matchesSearch(a, s.search))
                .sorted(s.sort.comparator)
                .collect(Collectors.toList());

        if (s.mode == Mode.PLAYER && list.isEmpty() && s.search.isEmpty() && s.category == Category.ALL) {
            openEmpty(p, s.targetName);
            return;
        }

        int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
        s.page = Math.max(0, Math.min(s.page, pages - 1));

        String t;
        if (s.mode == Mode.PLAYER) {
            t = PintosLeiloes.getInstance().getMessages().get("gui.titles.player", "player", s.targetName);
        } else if (!s.search.isEmpty()) {
            t = PintosLeiloes.getInstance().getMessages().get("gui.titles.market-search", "search", s.search);
        } else {
            t = PintosLeiloes.getInstance().getMessages().get("gui.titles.market");
        }

        ListMenu h = new ListMenu(s);
        h.pages = pages;
        Inventory inv = Bukkit.createInventory(h, 54, title(t));
        h.inv = inv;
        fill(inv, pane());

        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            Category c = cats[i];
            boolean sel = c == s.category;
            String label = PintosLeiloes.getInstance().getMessages().get("gui.categories." + c.name());
            ItemStack ic = Util.icon(c.icon, PintosLeiloes.getInstance().getMessages().get(sel ? "gui.category.selected" : "gui.category.unselected", "name", label), PintosLeiloes.getInstance().getMessages().get(sel ? "gui.category.lore-selected" : "gui.category.lore-unselected"));
            if (sel) {
                ItemMeta m = ic.getItemMeta();
                if (m != null) {
                    m.addEnchant(Enchantment.LURE, 1, true);
                    m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    ic.setItemMeta(m);
                }
            }
            inv.setItem(CATEGORY_SLOTS[i], ic);
        }

        for (int i = 0; i < PER_PAGE; i++) {
            int idx = s.page * PER_PAGE + i;
            if (idx >= list.size()) {
                break;
            }
            Auction a = list.get(idx);
            inv.setItem(ITEM_SLOTS[i], auctionIcon(a, false));
            h.ids.put(ITEM_SLOTS[i], a.id);
        }

        Object[] pg = {"pages", pages, "page", s.page + 1, "count", list.size(), "player", p.getName()};
        if (s.page > 0) {
            inv.setItem(SLOT_PREV, btn(Material.ARROW, "prev", cat(pg, "target", s.page)));
        }

        if (s.page < pages - 1) {
            inv.setItem(SLOT_NEXT, btn(Material.ARROW, "next", cat(pg, "target", s.page + 2)));
        }

        if (s.mode != Mode.MARKET) {
            inv.setItem(SLOT_BACK, btn(Material.BARRIER, "back", pg));
        }

        inv.setItem(SLOT_SEARCH, btn(Material.OAK_SIGN, "search", cat(pg, "search", s.search.isEmpty() ? PintosLeiloes.getInstance().getMessages().get("gui.no-search") : s.search)));
        inv.setItem(SLOT_SORT, btn(Material.HOPPER, "sort", cat(pg, "sort", PintosLeiloes.getInstance().getMessages().get("gui.sorts." + s.sort.name()))));
        inv.setItem(SLOT_REFRESH, btn(Material.ENDER_EYE, "refresh", pg));
        inv.setItem(SLOT_SELL, btn(Material.GOLD_INGOT, "sell", pg));
        inv.setItem(SLOT_WIN, btn(Material.CHEST, "win", cat(pg, "count", PintosLeiloes.getInstance().getAuctionManager().claimCount(p.getUniqueId()))));
        inv.setItem(SLOT_MINE, btn(Material.BOOK, "mine", pg));

        p.openInventory(inv);
    }

    private void openClaims(Player p, ListState s) {
        List<Claim> list = PintosLeiloes.getInstance().getAuctionManager().claims(p.getUniqueId());
        int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
        s.page = Math.max(0, Math.min(s.page, pages - 1));

        ListMenu h = new ListMenu(s);
        h.pages = pages;
        Inventory inv = Bukkit.createInventory(h, 54, title(PintosLeiloes.getInstance().getMessages().get("gui.titles.claims")));
        h.inv = inv;
        fill(inv, pane());

        for (int i = 0; i < PER_PAGE; i++) {
            int idx = s.page * PER_PAGE + i;
            if (idx >= list.size()) {
                break;
            }
            Claim c = list.get(idx);
            inv.setItem(ITEM_SLOTS[i], claimIcon(c));
            h.ids.put(ITEM_SLOTS[i], c.id);
        }

        Object[] pg = {"pages", pages, "page", s.page + 1, "count", list.size(), "player", p.getName()};

        if (s.page > 0) {
            inv.setItem(SLOT_PREV, btn(Material.ARROW, "prev", cat(pg, "target", s.page)));
        }
        if (s.page < pages - 1) {
            inv.setItem(SLOT_NEXT, btn(Material.ARROW, "next", cat(pg, "target", s.page + 2)));
        }

        inv.setItem(SLOT_BACK, btn(Material.BARRIER, "back", pg));
        inv.setItem(SLOT_REFRESH, btn(Material.ENDER_EYE, "refresh", pg));
        p.openInventory(inv);
    }

    public void openDetail(Player p, Auction a, ListState back) {
        DetailMenu h = new DetailMenu(a.id, back);
        Inventory inv = Bukkit.createInventory(h, 27, title(PintosLeiloes.getInstance().getMessages().get("gui.titles.detail", "id", a.id)));
        h.inv = inv;
        fill(inv, pane());
        Object[] ph = {"id", a.id, "seller", a.sellerName, "player", p.getName(), "min", Util.money(PintosLeiloes.getInstance().getAuctionManager().minNextBid(a)), "item", Util.itemName(a.item)};
        inv.setItem(D_BACK, btn(Material.ARROW, "detail-back", ph));
        inv.setItem(D_ITEM, auctionIcon(a, true));

        boolean isSeller = a.seller.equals(p.getUniqueId());
        if (isSeller) {
            if (!a.hasBids()) {
                inv.setItem(D_ACTION, btn(Material.RED_WOOL, "detail-cancel", ph));
            } else {
                inv.setItem(D_ACTION, btn(Material.BARRIER, "detail-cancel-blocked", ph));
            }
        } else {
            inv.setItem(D_ACTION, btn(Material.LIME_WOOL, "detail-buy", ph));
        }
        if (p.hasPermission("leilao.admin") && !isSeller) {
            inv.setItem(D_ADMIN, btn(Material.TNT, "detail-admin", ph));
        }
        inv.setItem(D_SELLER, btn(Material.PAPER, "detail-seller", ph));
        p.openInventory(inv);
    }

    public void openSell(Player p) {
        if (!p.hasPermission("leilao.bypass.limit") && PintosLeiloes.getInstance().getAuctionManager().countBySeller(p.getUniqueId()) >= Util.maxItems()) {
            PintosLeiloes.getInstance().getMessages().send(p, "sell.limit-reached", "player", p.getName(), "max", Util.maxItems());
            return;
        }
        SellMenu h = new SellMenu();
        Inventory inv = Bukkit.createInventory(h, 36, title(PintosLeiloes.getInstance().getMessages().get("gui.titles.sell")));
        h.inv = inv;
        ItemStack glass = Util.icon(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 27; i < 36; i++) {
            inv.setItem(i, glass);
        }
        Object[] ph = {"player", p.getName(), "max", Util.maxItems()};
        inv.setItem(SELL_CANCEL, btn(Material.RED_WOOL, "sell-cancel", ph));
        inv.setItem(SELL_INFO, btn(Material.PAPER, "sell-info", ph));
        inv.setItem(SELL_CONFIRM, btn(Material.LIME_WOOL, "sell-confirm", ph));
        p.openInventory(inv);
    }


    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getInventory();
        InventoryHolder h = top.getHolder();
        if (!(h instanceof BaseMenu)) {
            return;
        }
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        Player p = (Player) e.getWhoClicked();

        if (h instanceof SellMenu) {
            handleSell(e, p, (SellMenu) h, top);
            return;
        }

        e.setCancelled(true);
        if (e.getClickedInventory() == null || e.getClickedInventory() != top) {
            return;
        }
        int slot = e.getRawSlot();

        if (h instanceof ListMenu) {
            clickList(p, (ListMenu) h, slot, e.getClick());
        } else if (h instanceof DetailMenu) {
            clickDetail(p, (DetailMenu) h, slot);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        InventoryHolder h = e.getInventory().getHolder();
        if (h instanceof SellMenu) {
            for (int s : e.getRawSlots()) {
                if (s >= 27 && s < 36) {
                    e.setCancelled(true);
                    return;
                }
            }
        } else if (h instanceof BaseMenu) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        InventoryHolder h = e.getInventory().getHolder();
        if (!(h instanceof SellMenu)) {
            return;
        }
        SellMenu m = (SellMenu) h;
        if (m.confirmed) {
            return;
        }
        Player p = (Player) e.getPlayer();
        for (int i = 0; i < 27; i++) {
            ItemStack it = m.inv.getItem(i);
            if (it != null && it.getType() != Material.AIR) {
                Util.giveOrDrop(p, it);
                m.inv.setItem(i, null);
            }
        }
    }


    private void clickList(Player p, ListMenu m, int slot, ClickType click) {
        ListState s = m.state;

        if (s.mode == Mode.WIN) {
            if (slot == SLOT_PREV && s.page > 0) {
                s.page--;
                later(() -> openList(p, s));
                return;
            }

            if (slot == SLOT_NEXT && s.page < m.pages - 1) {
                s.page++;
                later(() -> openList(p, s));
                return;
            }

            if (slot == SLOT_BACK) {
                later(() -> openMarket(p));
                return;
            }

            if (slot == SLOT_REFRESH) {
                later(() -> openList(p, s));
                return;
            }

            Integer claimId = m.ids.get(slot);
            if (claimId != null) {
                PintosLeiloes.getInstance().getAuctionManager().claimItem(p, claimId);
                later(() -> openList(p, s));
            }
            return;
        }

        for (int i = 0; i < CATEGORY_SLOTS.length; i++) {
            if (slot == CATEGORY_SLOTS[i]) {
                s.category = Category.values()[i];
                s.page = 0;
                later(() -> openList(p, s));
                return;
            }
        }

        if (slot == SLOT_PREV && s.page > 0) {
            s.page--;
            later(() -> openList(p, s));
            return;
        }

        if (slot == SLOT_NEXT && s.page < m.pages - 1) {
            s.page++;
            later(() -> openList(p, s));
            return;
        }

        if (slot == SLOT_BACK && s.mode != Mode.MARKET) {
            later(() -> openMarket(p));
            return;
        }

        if (slot == SLOT_REFRESH) {
            later(() -> openList(p, s));
            return;
        }

        if (slot == SLOT_SORT) {
            s.sort = s.sort.next();
            s.page = 0;
            later(() -> openList(p, s));
            return;
        }

        if (slot == SLOT_SELL) {
            later(() -> openSell(p));
            return;
        }

        if (slot == SLOT_WIN) {
            later(() -> openWin(p));
            return;
        }

        if (slot == SLOT_MINE) {
            later(() -> openPlayer(p, p.getName()));
            return;
        }

        if (slot == SLOT_SEARCH) {
            if (click.isRightClick()) {
                s.search = "";
                s.page = 0;
                later(() -> openList(p, s));
            } else {
                later(() -> {
                    p.closeInventory();
                    PintosLeiloes.getInstance().getInputManager().startSearch(p, s);
                });
            }
            return;
        }

        Integer id = m.ids.get(slot);
        if (id != null) {
            Auction a = PintosLeiloes.getInstance().getAuctionManager().get(id);
            if (a == null) {
                PintosLeiloes.getInstance().getMessages().send(p, "general.auction-ended", "player", p.getName());
                later(() -> openList(p, s));
                return;
            }
            later(() -> openDetail(p, a, s));
        }
    }

    private void clickDetail(Player p, DetailMenu m, int slot) {
        Auction a = PintosLeiloes.getInstance().getAuctionManager().get(m.auctionId);
        if (slot == D_BACK) {
            later(() -> {
                if (m.back != null) {
                    openList(p, m.back);
                } else {
                    openMarket(p);
                }
            });
            return;
        }
        if (a == null) {
            PintosLeiloes.getInstance().getMessages().send(p, "general.auction-ended", "player", p.getName());
            later(() -> openMarket(p));
            return;
        }
        boolean isSeller = a.seller.equals(p.getUniqueId());
        if (slot == D_SELLER) {
            later(() -> openPlayer(p, a.sellerName));
        } else if (slot == D_ACTION) {
            if (isSeller) {
                if (a.hasBids()) {
                    return;
                }
                PintosLeiloes.getInstance().getAuctionManager().cancel(a, p);
                later(() -> openPlayer(p, p.getName()));
            } else {
                later(() -> {
                    p.closeInventory();
                    PintosLeiloes.getInstance().getInputManager().startBid(p, a, m.back);
                });
            }
        } else if (slot == D_ADMIN && p.hasPermission("leilao.admin") && !isSeller) {
            PintosLeiloes.getInstance().getAuctionManager().cancel(a, p);
            PintosLeiloes.getInstance().getMessages().send(p, "cancel.admin-done", "player", p.getName(), "id", a.id, "item", Util.itemName(a.item), "seller", a.sellerName);
            later(() -> openMarket(p));
        }
    }

    private void handleSell(InventoryClickEvent e, Player p, SellMenu m, Inventory top) {
        if (e.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            e.setCancelled(true);
            return;
        }

        if (e.getClickedInventory() != top) {
            return;
        }

        int slot = e.getRawSlot();

        if (slot < 27) {
            return;
        }

        e.setCancelled(true);
        if (slot == SELL_CONFIRM) {
            confirmSell(p, m);
        } else if (slot == SELL_CANCEL) {
            later(p::closeInventory);
        }
    }

    private void confirmSell(Player p, SellMenu m) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 27; i++) {
            ItemStack it = m.inv.getItem(i);
            if (it != null && it.getType() != Material.AIR) {
                items.add(it.clone());
            }
        }
        if (items.isEmpty()) {
            PintosLeiloes.getInstance().getMessages().send(p, "sell.no-items", "player", p.getName());
            return;
        }
        for (ItemStack it : items) {
            if (Util.isBanned(it)) {
                PintosLeiloes.getInstance().getMessages().send(p, "sell.banned-item-remove", "player", p.getName(), "item", Util.itemName(it));
                return;
            }
        }
        if (!p.hasPermission("leilao.bypass.limit") && PintosLeiloes.getInstance().getAuctionManager().countBySeller(p.getUniqueId()) + items.size() > Util.maxItems()) {
            PintosLeiloes.getInstance().getMessages().send(p, "sell.limit-remaining", "player", p.getName(), "max", Util.maxItems(), "remaining", Math.max(0, Util.maxItems() - PintosLeiloes.getInstance().getAuctionManager().countBySeller(p.getUniqueId())));
            return;
        }
        m.confirmed = true;
        for (int i = 0; i < 27; i++) {
            m.inv.setItem(i, null);
        }
        later(() -> {
            p.closeInventory();
            PintosLeiloes.getInstance().getInputManager().startSell(p, items);
        });
    }

    private static Object[] cat(Object[] a, Object... b) {
        Object[] r = new Object[a.length + b.length];
        System.arraycopy(b, 0, r, 0, b.length);
        System.arraycopy(a, 0, r, b.length, a.length);
        return r;
    }
}
