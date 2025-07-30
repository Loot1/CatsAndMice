package fr.loot1.catsandmice.managers;

import fr.loot1.catsandmice.CatsAndMice;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.List;

public class ConfigManager {

    private final CatsAndMice main;
    private final File configFile;
    private final FileConfiguration config;

    public ConfigManager(CatsAndMice catsAndMice) {
        this.main = catsAndMice;
        this.configFile = new File(catsAndMice.getDataFolder(), "config.yml");
        this.config = catsAndMice.getConfig();
        init();
    }

    private void create() {
        main.saveDefaultConfig();
        main.getConfig().options().copyDefaults(true);
        main.getLogger().info("Configuration file added");
    }

    private void init() {
        if(!configFile.exists()) {
            create();
        }
    }

    public void reload() {
        if(configFile.exists()) {
            main.getLogger().info("Configuration file reloaded");
        } else {
            create();
        }
        main.reloadConfig();
    }

    public List<String> getColoredList(final String path) {
        if(this.config.contains(path)) {
            List<String> messages = this.config.getStringList(path);
            messages.replaceAll(msgToColor -> ChatColor.translateAlternateColorCodes('&', msgToColor));
            return messages;
        }
        main.getLogger().severe("This value doesn't exist in configuration file :" + path);
        return null;
    }

    public String getColored(final String path) {
        if(this.config.contains(path)) {
            return ChatColor.translateAlternateColorCodes('&', this.config.getString(path));
        }
        main.getLogger().severe("This value doesn't exist in configuration file :" + path);
        return "";
    }

    public String getColoredReplaced(final String path, final String[] toReplace, final String[] replacement) {
        if(config.contains(path)) {
            String message = config.getString(path);
            for (int i = 0; i < toReplace.length; i++) {
                message = message.replaceAll("%" + toReplace[i] + "%", replacement[i]);
            }
            return ChatColor.translateAlternateColorCodes('&', message);
        }
        main.getLogger().severe("This value doesn't exist in configuration file : " + path);
        return "";
    }

    public List<String> getColoredListReplaced(final String path, final String[] toReplace, final String[] replacement) {
        if (config.contains(path)) {
            List<String> messages = config.getStringList(path);
            for (int i = 0; i < toReplace.length; i++) {
                int finalI = i;
                messages.replaceAll(msgToPlaceHolderised -> msgToPlaceHolderised.replaceAll("%" + toReplace[finalI] + "%", replacement[finalI]));
            }
            messages.replaceAll(msgToColor -> ChatColor.translateAlternateColorCodes('&', msgToColor));
            return messages;
        } else {
            main.getLogger().severe("This value doesn't exist in configuration file: " + path);
        }
        return null;
    }

    public boolean getBoolean(final String path) {
        if(this.config.contains(path)) {
            return this.config.getBoolean(path);
        }
        main.getLogger().severe("This value doesn't exist in configuration file :" + path);
        return false;
    }

    public int getInt(final String path) {
        return this.config.getInt(path);
    }

    public Long getLong(final String path) {
        return this.config.getLong(path);
    }

    public String get(final String path) {
        if(this.config.contains(path)) {
            return this.config.getString(path);
        }
        main.getLogger().severe("This value doesn't exist in configuration file :" + path);
        return null;
    }

}
