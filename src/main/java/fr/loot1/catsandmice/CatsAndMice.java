package fr.loot1.catsandmice;

import fr.loot1.catsandmice.commands.CatsAndMiceCommand;
import fr.loot1.catsandmice.listeners.HologramClickListener;
import fr.loot1.catsandmice.managers.DataFileManager;
import fr.loot1.catsandmice.managers.ConfigManager;
import fr.loot1.catsandmice.managers.GameManager;
import fr.loot1.catsandmice.managers.HologramManager;
import fr.loot1.catsandmice.utils.RanksHelper;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.plugin.java.annotation.dependency.Dependency;
import org.bukkit.plugin.java.annotation.dependency.SoftDependency;
import org.bukkit.plugin.java.annotation.permission.ChildPermission;
import org.bukkit.plugin.java.annotation.permission.Permission;
import org.bukkit.plugin.java.annotation.plugin.ApiVersion;
import org.bukkit.plugin.java.annotation.plugin.Description;
import org.bukkit.plugin.java.annotation.plugin.LogPrefix;
import org.bukkit.plugin.java.annotation.plugin.Plugin;
import org.bukkit.plugin.java.annotation.plugin.author.Author;

@Plugin(name = "CatsAndMice", version = "1.0.4")
@Description("Le chat et la souris avec des modos !")
@Author("Loot1")
@Author("Eniox59")
@LogPrefix("CatsAndMice")
@Dependency("DecentHolograms")
@SoftDependency("LuckPerms")
@ApiVersion(ApiVersion.Target.v1_20)

@Permission(name = "catsandmice.*", desc = "Allows all permissions of CatsAndMice plugin", defaultValue = PermissionDefault.OP, children = {
        @ChildPermission(name ="catsandmice.bypass"),
        @ChildPermission(name ="catsandmice.cat"),
        @ChildPermission(name ="catsandmice.create"),
        @ChildPermission(name ="catsandmice.help"),
        @ChildPermission(name ="catsandmice.mice"),
        @ChildPermission(name ="catsandmice.reload"),
})
@Permission(name = "catsandmice.bypass", desc = "Permet de contourner le délai entre les clics", defaultValue = PermissionDefault.OP)
@Permission(name = "catsandmice.cat", desc = "Permet de réinitialiser le score en cliquant sur l'hologramme", defaultValue = PermissionDefault.OP)
@Permission(name = "catsandmice.mice", desc = "Permet de jouer au jeu en cliquant sur l'hologramme", defaultValue = PermissionDefault.OP)

public class CatsAndMice extends JavaPlugin {

    private ConfigManager configManager;
    private DataFileManager dataFileManager;
    private GameManager gameManager;
    private HologramManager hologramManager;
    private RanksHelper ranksHelper;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        dataFileManager = new DataFileManager(this, "data.yml");

        if(!getServer().getPluginManager().isPluginEnabled("DecentHolograms")) {
            getLogger().severe("DecentHolograms is not enabled, disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        ranksHelper = new RanksHelper();
        hologramManager = new HologramManager(this, ranksHelper);
        gameManager = new GameManager(this, hologramManager);

        getCommand("mice").setExecutor(new CatsAndMiceCommand(this));
        getServer().getPluginManager().registerEvents(new HologramClickListener(this), this);
    }

    @Override
    public void onDisable() {
        if (dataFileManager != null) {
            dataFileManager.saveSync();
            getLogger().info("Données sauvegardées.");
        }
    }

    public ConfigManager getConfigManager() { return configManager; }

    public DataFileManager getDataFileManager() { return dataFileManager; }

    public GameManager getGameManager() { return gameManager; }

    public HologramManager getHologramManager() { return hologramManager; }

    public RanksHelper getRanksHelper() { return ranksHelper; }

}