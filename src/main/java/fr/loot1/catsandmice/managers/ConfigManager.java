package fr.loot1.catsandmice.managers;

import fr.loot1.catsandmice.CatsAndMice;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigManager {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    private final CatsAndMice main;
    private final File configFile;

    public ConfigManager(CatsAndMice catsAndMice) {
        this.main = catsAndMice;
        this.configFile = new File(catsAndMice.getDataFolder(), "config.yml");
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

    private FileConfiguration cfg() {
        return main.getConfig();
    }

    private Component parse(String text) {
        return MINI_MESSAGE.deserialize(text);
    }

    private String toLegacy(String text) {
        return LEGACY_SERIALIZER.serialize(parse(text));
    }

    public Component getComponent(final String path) {
        if (cfg().contains(path)) {
            return parse(cfg().getString(path, ""));
        }
        main.getLogger().severe("This value doesn't exist in configuration file: " + path);
        return Component.empty();
    }

    public Component getComponentReplaced(final String path, final String[] toReplace, final String[] replacement) {
        if (cfg().contains(path)) {
            String text = cfg().getString(path, "");
            for (int i = 0; i < toReplace.length; i++) {
                text = text.replace("%" + toReplace[i] + "%", replacement[i]);
            }
            return parse(text);
        }
        main.getLogger().severe("This value doesn't exist in configuration file: " + path);
        return Component.empty();
    }

    public List<Component> getComponentList(final String path) {
        if (cfg().contains(path)) {
            return cfg().getStringList(path).stream()
                    .map(this::parse)
                    .collect(Collectors.toList());
        }
        main.getLogger().severe("This value doesn't exist in configuration file: " + path);
        return Collections.emptyList();
    }

    public List<Component> getComponentListReplaced(final String path, final String[] toReplace, final String[] replacement) {
        if (cfg().contains(path)) {
            List<String> messages = cfg().getStringList(path);
            return messages.stream().map(text -> {
                String processed = text;
                for (int i = 0; i < toReplace.length; i++) {
                    processed = processed.replace("%" + toReplace[i] + "%", replacement[i]);
                }
                return parse(processed);
            }).collect(Collectors.toList());
        }
        main.getLogger().severe("This value doesn't exist in configuration file: " + path);
        return Collections.emptyList();
    }

    public String getColored(final String path) {
        if (cfg().contains(path)) {
            return toLegacy(cfg().getString(path, ""));
        }
        main.getLogger().severe("This value doesn't exist in configuration file: " + path);
        return "";
    }

    public String getColoredReplaced(final String path, final String[] toReplace, final String[] replacement) {
        if (cfg().contains(path)) {
            String text = cfg().getString(path, "");
            for (int i = 0; i < toReplace.length; i++) {
                text = text.replace("%" + toReplace[i] + "%", replacement[i]);
            }
            return toLegacy(text);
        }
        main.getLogger().severe("This value doesn't exist in configuration file: " + path);
        return "";
    }

    public List<String> getColoredList(final String path) {
        if (cfg().contains(path)) {
            return cfg().getStringList(path).stream()
                    .map(this::toLegacy)
                    .collect(Collectors.toList());
        }
        main.getLogger().severe("This value doesn't exist in configuration file: " + path);
        return Collections.emptyList();
    }

    public List<String> getColoredListReplaced(final String path, final String[] toReplace, final String[] replacement) {
        if (cfg().contains(path)) {
            List<String> messages = cfg().getStringList(path);
            return messages.stream().map(text -> {
                String processed = text;
                for (int i = 0; i < toReplace.length; i++) {
                    processed = processed.replace("%" + toReplace[i] + "%", replacement[i]);
                }
                return toLegacy(processed);
            }).collect(Collectors.toList());
        }
        main.getLogger().severe("This value doesn't exist in configuration file: " + path);
        return Collections.emptyList();
    }

    public boolean getBoolean(final String path) {
        if (cfg().contains(path)) {
            return cfg().getBoolean(path);
        }
        main.getLogger().severe("This value doesn't exist in configuration file: " + path);
        return false;
    }

    public int getInt(final String path) {
        return cfg().getInt(path);
    }

    public Long getLong(final String path) {
        return cfg().getLong(path);
    }

    public String get(final String path) {
        if (cfg().contains(path)) {
            return cfg().getString(path);
        }
        main.getLogger().severe("This value doesn't exist in configuration file: " + path);
        return null;
    }

    public void reload() {
        if (configFile.exists()) {
            main.getLogger().info("Configuration file reloaded");
        } else {
            create();
        }
        main.reloadConfig();
    }
}