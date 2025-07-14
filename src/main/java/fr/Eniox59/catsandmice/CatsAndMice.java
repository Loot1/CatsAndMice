package fr.Eniox59.catsandmice;

import fr.Eniox59.catsandmice.commands.CatsAndMiceCommand;
import fr.Eniox59.catsandmice.listeners.HologramClickListener;
import fr.Eniox59.catsandmice.managers.GameManager;
import org.bukkit.plugin.java.JavaPlugin;

public class CatsAndMice extends JavaPlugin {
    
    private static CatsAndMice instance;
    private GameManager gameManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Message de démarrage
        getLogger().info("");
        getLogger().info("=== CatsAndMice v" + getDescription().getVersion() + " ===");
        getLogger().info("Developpe par " + String.join(", ", getDescription().getAuthors()));
        getLogger().info("");
        
        // Forcer l'encodage UTF-8 pour la console
        System.setProperty("file.encoding", "UTF-8");
        try {
            java.lang.reflect.Field charset = java.nio.charset.Charset.class.getDeclaredField("defaultCharset");
            charset.setAccessible(true);
            charset.set(null, null);
        } catch (Exception e) {
            // Ignorer les erreurs de reflection
        }
        
        try {
            // Vérification de la dépendance DecentHolograms
            if (!getServer().getPluginManager().isPluginEnabled("DecentHolograms")) {
                getLogger().severe("ERREUR: DecentHolograms n'est pas installe ou active !");
                getLogger().severe("Veuillez installer DecentHolograms pour utiliser ce plugin.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // Sauvegarder la config par défaut si elle n'existe pas
            saveDefaultConfig();
            reloadConfig();
            
            // Initialisation des gestionnaires
            this.gameManager = new GameManager(this);
            
            // Enregistrement des commandes
            getCommand("mice").setExecutor(new CatsAndMiceCommand(this));
            
            // Enregistrement des événements
            getServer().getPluginManager().registerEvents(new HologramClickListener(this), this);
            
            getLogger().info("Pret a l'emploi !");
            getLogger().info("Utilisez /mice create pour creer un hologramme");
            
        } catch (Exception e) {
            getLogger().severe("Erreur lors du chargement: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        getLogger().info("Arrêt de CatsAndMice...");
        
        try {
            // Sauvegarder les données à l'arrêt
            if (gameManager != null) {
                gameManager.saveData();
                getLogger().info("§aDonnées sauvegardées avec succès");
            }
            
            // Sauvegarder la configuration
            saveConfig();
            
            getLogger().info("§6Plugin désactivé");
            getLogger().info("§cLe plugin a été désactivé.");
        } catch (Exception e) {
            getLogger().severe("§cErreur lors de l'arrêt du plugin: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static CatsAndMice getInstance() {
        return instance;
    }
    
    public GameManager getGameManager() {
        return gameManager;
    }
    
    @Override
    public void reloadConfig() {
        super.reloadConfig();
        // Recharger les messages et configurations personnalisées ici si nécessaire
        getLogger().info("Configuration rechargee avec succes !");
    }
}
