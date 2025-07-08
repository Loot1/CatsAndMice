package fr.Eniox59.clickgame.commands;

import fr.Eniox59.clickgame.ClickGame;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClickGameCommand implements CommandExecutor {
    
    private final ClickGame plugin;
    
    public ClickGameCommand(ClickGame plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cCette commande ne peut être exécutée que par un joueur !");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Vérifier la permission de base
        if (!player.hasPermission("clickgame.command")) {
            player.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande !");
            return true;
        }
        
        // Gestion des sous-commandes
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "create":
                handleCreateCommand(player);
                break;
                
            case "reload":
                handleReloadCommand(player);
                break;
                
            default:
                sendHelp(player);
                break;
        }
        
        return true;
    }
    
    private void handleCreateCommand(Player player) {
        if (!player.hasPermission("clickgame.command")) {
            player.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande !");
            return;
        }
        
        player.sendMessage("§eCréation de l'hologramme en cours...");
        
        try {
            // Vérification de la disponibilité de l'API DecentHolograms
            if (plugin.getServer().getPluginManager().getPlugin("DecentHolograms") == null) {
                throw new IllegalStateException("DecentHolograms n'est pas disponible ou n'est pas activé");
            }
            
            // Création de l'hologramme
            plugin.getGameManager().createHologram(player.getLocation());
            
            String locationStr = String.format("monde: %s, x: %d, y: %d, z: %d",
                player.getLocation().getWorld().getName(),
                player.getLocation().getBlockX(),
                player.getLocation().getBlockY(),
                player.getLocation().getBlockZ());
            
            player.sendMessage("§a✅ Hologramme créé avec succès !");
            player.sendMessage("§7Emplacement: §f" + locationStr);
            
            // Log de la création de l'hologramme (sans caractères spéciaux) si activé dans la config
            if (plugin.getConfig().getBoolean("settings.debug.log-hologram-update", false)) {
                plugin.getLogger().info(String.format("[HOLOGRAM] Cree par %s - %s", 
                    player.getName(), locationStr));
            }
                
        } catch (Exception e) {
            player.sendMessage("§c❌ Erreur lors de la création de l'hologramme !");
            player.sendMessage("§cDétails: §7" + e.getMessage());
            
            plugin.getLogger().severe("Erreur lors de la création de l'hologramme:");
            plugin.getLogger().severe("Joueur: " + player.getName());
            plugin.getLogger().severe("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleReloadCommand(Player player) {
        if (!player.hasPermission("clickgame.command")) {
            player.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande !");
            return;
        }
        
        try {
            plugin.reloadConfig();
            player.sendMessage("§a✅ Configuration rechargée avec succès !");
            // Utilisation de la constante de couleur pour éviter les problèmes d'encodage
            plugin.getLogger().info("Configuration rechargee par " + player.getName());
        } catch (Exception e) {
            player.sendMessage("§c❌ Erreur lors du rechargement de la configuration !");
            plugin.getLogger().severe("Erreur lors du rechargement de la configuration:");
            e.printStackTrace();
        }
    }
    
    private void sendHelp(Player player) {
        player.sendMessage("");
        player.sendMessage("§6§l=== §eAide de ClickGame §6§l===");
        player.sendMessage("");
        
        if (player.hasPermission("clickgame.command")) {
            player.sendMessage("§6/clickgame create");
            player.sendMessage(" §7Crée un nouvel hologramme à votre position");
            player.sendMessage("");
            
            player.sendMessage("§6/clickgame reload");
            player.sendMessage(" §7Recharge la configuration du plugin");
            player.sendMessage("");
            
            if (player.hasPermission("clickgame.bypass")) {
                player.sendMessage("§6Permission spéciale :");
                player.sendMessage(" §7- §eclickgame.bypass §7: Pas de délai entre les clics");
                player.sendMessage("");
            }
        }
        
        // Afficher les informations de version
        player.sendMessage("§7Version: §f" + plugin.getDescription().getVersion());
        player.sendMessage("");
    }
}
