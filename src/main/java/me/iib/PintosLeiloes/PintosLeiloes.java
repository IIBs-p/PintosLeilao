package me.iib.PintosLeiloes;

import me.iib.PintosLeiloes.commands.LeilaoCommands;
import me.iib.PintosLeiloes.config.Messages;
import me.iib.PintosLeiloes.database.Database;
import me.iib.PintosLeiloes.manager.AuctionManager;
import me.iib.PintosLeiloes.manager.InputManager;
import me.iib.PintosLeiloes.manager.MenuManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Material;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class PintosLeiloes extends JavaPlugin {
    private static PintosLeiloes instance;

    private Messages messages;
    private Database databaseManager;
    private AuctionManager auctionManager;
    private MenuManager menuManager;
    private InputManager inputManager;
    private Economy economy;

    private final Set<Material> banned = new HashSet<>();

    @Override
    public void onLoad() {

    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        messages = new Messages();
        reloadSettings();

        databaseManager = new Database();
        try {
            databaseManager.connect();
        } catch (Exception e) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        auctionManager = new AuctionManager();
        auctionManager.loadActive();
        menuManager = new MenuManager();
        inputManager = new InputManager();

        getServer().getPluginManager().registerEvents(menuManager, this);
        getServer().getPluginManager().registerEvents(inputManager, this);

        LeilaoCommands cmd = new LeilaoCommands();
        getCommand("leilao").setExecutor(cmd);
        getCommand("leilao").setTabCompleter(cmd);

        getServer().getScheduler().runTaskTimer(this, auctionManager::tick, 20L, 20L);

        getServer().getScheduler().runTask(this, () -> {
            if (getEconomy() == null) {
                getLogger().warning(".");
            }
        });

    }

    @Override
    public void onDisable() {

    }

    public void reloadSettings() {
        reloadConfig();
        messages.load();
        banned.clear();
        for (String s : getConfig().getStringList("banned-items")) {
            Material m = Material.matchMaterial(s.toUpperCase(Locale.ROOT));
            if (m != null) {
                banned.add(m);
            }
        }
    }

    public Economy getEconomy() {
        if (economy == null) {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
            }
        }
        return economy;
    }

    public static PintosLeiloes getInstance() {
        return instance;
    }

    public Messages getMessages() {
        return messages;
    }

    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }

    public InputManager getInputManager() {
        return inputManager;
    }

    public Set<Material> getBanned() {
        return banned;
    }

    public Database getDatabaseManager() {
        return databaseManager;
    }
}
