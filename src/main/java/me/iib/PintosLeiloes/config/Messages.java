package me.iib.PintosLeiloes.config;

import me.iib.PintosLeiloes.PintosLeiloes;
import me.iib.PintosLeiloes.utils.Util;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Written by iib <iib@realmclub.com>, 15:48 27/10/2026
 */
public class Messages {
    private YamlConfiguration cfg;

    public void load() {
        File f = new File(PintosLeiloes.getInstance().getDataFolder(), "messages.yml");
        if (!f.exists()) {
            PintosLeiloes.getInstance().saveResource("messages.yml", false);
        }

        cfg = YamlConfiguration.loadConfiguration(f);

        InputStream in = PintosLeiloes.getInstance().getResource("messages.yml");
        if (in != null) {
            cfg.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
        }
        Util.setTimeFormat(get("time.days"), get("time.hours"), get("time.minutes"), get("time.seconds"), get("time.ended"), get("time.item-amount"));
    }

    private String apply(String s, Object... ph) {
        s = ChatColor.translateAlternateColorCodes('&', s);
        for (int i = 0; i + 1 < ph.length; i += 2) {
            s = s.replace("{" + ph[i] + "}", String.valueOf(ph[i + 1]));
        }
        return s;
    }

    public String get(String key, Object... ph) {
        String raw = cfg.getString(key);
        if (raw == null) {
            return "";
        }

        return apply(raw, ph);
    }

    public List<String> list(String key, Object... ph) {
        List<String> out = new ArrayList<>();
        for (String s : cfg.getStringList(key)) {
            out.add(apply(s, ph));
        }

        return out;
    }

    public void send(CommandSender player, String key, Object... ph) {
        String s = get(key, ph);
        if (!s.isEmpty()) {
            player.sendMessage(s);
        };
    }
}
