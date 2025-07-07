package fr.loot1.catsAndMice.utils;

import java.io.File;
import java.util.List;

import fr.loot1.catsAndMice.CatsAndMice;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

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
        main.getLogger().info("Configuration file added");
    }

    public void init() {
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

    public Location getLocation(final String path) {
        if(this.config.contains(path)) {
            return this.config.getLocation(path);
        }
        main.getLogger().severe("This value doesn't exist in configuration file :" + path);
        return null;
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

    public String get(final String path) {
        if(this.config.contains(path)) {
            return this.config.getString(path);
        }
        main.getLogger().severe("This value doesn't exist in configuration file :" + path);
        return null;
    }

}