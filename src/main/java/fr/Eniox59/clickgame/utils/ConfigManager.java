package fr.Eniox59.clickgame.utils;

import java.io.File;
import java.util.List;

import fr.Eniox59.clickgame.ClickGame;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final ClickGame plugin;
    private final File configFile;
    private final FileConfiguration config;

    public ConfigManager(ClickGame plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.config = plugin.getConfig();
        init();
    }

    private void create() {
        plugin.saveDefaultConfig();
        plugin.getLogger().info("Configuration file added");
    }

    public void init() {
        if(!configFile.exists()) {
            create();
        }
    }

    public void reload() {
        if(configFile.exists()) {
            plugin.getLogger().info("Configuration file reloaded");
        } else {
            create();
        }
        plugin.reloadConfig();
    }

    public Location getLocation(final String path) {
        if(this.config.contains(path)) {
            return this.config.getLocation(path);
        }
        plugin.getLogger().severe("This value doesn't exist in configuration file :" + path);
        return null;
    }

    public List<String> getColoredList(final String path) {
        if(this.config.contains(path)) {
            List<String> messages = this.config.getStringList(path);
            messages.replaceAll(msgToColor -> ChatColor.translateAlternateColorCodes('&', msgToColor));
            return messages;
        }
        plugin.getLogger().severe("This value doesn't exist in configuration file :" + path);
        return null;
    }

    public String getColored(final String path) {
        if(this.config.contains(path)) {
            return ChatColor.translateAlternateColorCodes('&', this.config.getString(path));
        }
        plugin.getLogger().severe("This value doesn't exist in configuration file :" + path);
        return "";
    }

    public boolean getBoolean(final String path) {
        if(this.config.contains(path)) {
            return this.config.getBoolean(path);
        }
        plugin.getLogger().severe("This value doesn't exist in configuration file :" + path);
        return false;
    }

    public int getInt(final String path) {
        return this.config.getInt(path);
    }

    public String get(final String path) {
        if(this.config.contains(path)) {
            return this.config.getString(path);
        }
        plugin.getLogger().severe("This value doesn't exist in configuration file :" + path);
        return null;
    }

    public void set(String path, Object value) {
        plugin.getConfig().set(path, value);
        plugin.saveConfig();
    }
}