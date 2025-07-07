package fr.loot1.catsAndMice;

import fr.loot1.catsAndMice.listeners.HologramClickListener;
import fr.loot1.catsAndMice.utils.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class CatsAndMice extends JavaPlugin {

    private ConfigManager configManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);

        if(!getServer().getPluginManager().isPluginEnabled("DecentHolograms")) {
            getLogger().severe("DecentHolograms is not enabled");
            getServer().getPluginManager().disablePlugin(this);
        }

        getServer().getPluginManager().registerEvents(new HologramClickListener(), this);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

}
