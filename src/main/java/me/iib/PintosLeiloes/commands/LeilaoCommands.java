package me.iib.PintosLeiloes.commands;

import me.iib.PintosLeiloes.PintosLeiloes;
import me.iib.PintosLeiloes.utils.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Written by iib <iib@realmclub.com>, 15:25 27/10/2026
 */
public class LeilaoCommands implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload") && sender.hasPermission("leilao.admin")) {
                PintosLeiloes.getInstance().reloadSettings();
                sender.sendMessage(PintosLeiloes.getInstance().getMessages().get("general.console-reloaded"));
            } else {
                sender.sendMessage(PintosLeiloes.getInstance().getMessages().get("general.players-only"));
            }
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("leilao.use")) {
            PintosLeiloes.getInstance().getMessages().send(p, "general.no-permission", "player", p.getName());
            return true;
        }
        if (PintosLeiloes.getInstance().getInputManager().has(p)) {
            PintosLeiloes.getInstance().getMessages().send(p, "general.busy", "player", p.getName(), "cancel", Util.cancelKeyword());
            return true;
        }

        if (args.length == 0) {
            PintosLeiloes.getInstance().getMenuManager().openMarket(p);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "sell":
            case "vender":
                PintosLeiloes.getInstance().getMenuManager().openSell(p);
                return true;
            case "win":
            case "ganhos":
                PintosLeiloes.getInstance().getMenuManager().openWin(p);
                return true;
            case "reload":
                if (!p.hasPermission("leilao.admin")) {
                    PintosLeiloes.getInstance().getMessages().send(p, "general.no-permission", "player", p.getName());
                    return true;
                }
                PintosLeiloes.getInstance().reloadSettings();
                PintosLeiloes.getInstance().getMessages().send(p, "general.reloaded", "player", p.getName());
                return true;
            case "view":
                if (args.length < 2) {
                    return true;
                }
                try {
                    if (PintosLeiloes.getInstance().getAuctionManager().get(Integer.parseInt(args[1])) == null || PintosLeiloes.getInstance().getAuctionManager().get(Integer.parseInt(args[1])).endTime <= System.currentTimeMillis()) {
                        PintosLeiloes.getInstance().getMessages().send(p, "general.auction-ended", "player", p.getName());
                    } else {
                        PintosLeiloes.getInstance().getMenuManager().openDetail(p, PintosLeiloes.getInstance().getAuctionManager().get(Integer.parseInt(args[1])), null);
                    }
                } catch (NumberFormatException ex) {
                    PintosLeiloes.getInstance().getMessages().send(p, "general.invalid-auction", "player", p.getName());
                }
                return true;
            default:
                PintosLeiloes.getInstance().getMenuManager().openPlayer(p, args[0]);
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length != 1) {
            return out;
        }
        String start = args[0].toLowerCase();
        List<String> all = new ArrayList<>(List.of("sell", "win"));

        if (sender.hasPermission("leilao.admin")) {
            all.add("reload");
        }

        for (Player o : Bukkit.getOnlinePlayers()) {
            all.add(o.getName());
        }
        for (String n : PintosLeiloes.getInstance().getAuctionManager().sellerNames()) {
            if (!all.contains(n)) {
                all.add(n);
            }
        }
        for (String s : all) {
            if (s.toLowerCase().startsWith(start)) {
                out.add(s);
            }
        }
        return out;
    }
}