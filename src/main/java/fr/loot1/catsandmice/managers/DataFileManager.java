package fr.loot1.catsandmice.managers;

import fr.loot1.catsandmice.CatsAndMice;
import fr.loot1.catsandmice.Click;
import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataFileManager {

    private final CatsAndMice main;
    private final File configFile;
    private YamlConfiguration config;
    private final String name;

    public DataFileManager(CatsAndMice catsAndMice, String name) {
        this.main = catsAndMice;
        this.name = name;
        this.configFile = new File(main.getDataFolder(), name);
        init();
    }

    private void create() {
        main.saveResource(name, false);
        main.getLogger().info("Data file added");
    }

    private void init() {
        if(!configFile.exists()) {
            create();
        }
        reload();
    }

    private void save() {
        main.getServer().getScheduler().runTaskAsynchronously(main, () -> {
            try {
                config.save(configFile);
            } catch (IOException e) {
                main.getLogger().severe("Erreur lors de la sauvegarde de " + name + " : " + e.getMessage());
            }
        });
    }

    public void reload() {
        config = new YamlConfiguration();
        try {
            config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            main.getLogger().severe("Erreur lors du chargement de " + name + " : " + e.getMessage());
        }
    }

    //
    // GETTERS & SETTERS
    //

    public int getInt(String path) { return config.getInt(path); }

    public List<String> getStringList(String path) { return config.getStringList(path); }

    public boolean exist(String path) {
        return config.contains(path);
    }

    public Location getLocation(String path) {
        return config.getLocation(path);
    }

    public void set(String path, Object value) {
        config.set(path, value);
        save();
    }

    // GAME

    public List<Click> getClicks(String path) {
        if (config.contains(path)) {
            List<Map<?, ?>> clickMaps = config.getMapList(path);
            List<Click> clicks = clickMaps.stream()
                    .map(map -> new Click(
                            UUID.fromString((String) map.get("uuid")),
                            (Long) map.get("date"),
                            (Integer) map.get("score")))
                    .toList();
            return new ArrayList<>(clicks);
        }
        main.getLogger().severe("This value doesn't exist in data file: " + path);
        return new ArrayList<>();
    }

    public void updateClicks(String path, List<Click> clicks) {
        List<Map<String, Object>> serializedClicks = new ArrayList<>();
        for (Click click : clicks) {
            Map<String, Object> map = new HashMap<>();
            map.put("uuid", String.valueOf(click.getUUID()));
            map.put("date", click.getDate());
            map.put("score", click.getScore());
            serializedClicks.add(map);
        }
        config.set(path, serializedClicks);
        save();
    }

}
