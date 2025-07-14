package fr.Eniox59.catsandmice.commands;

import fr.Eniox59.catsandmice.CatsAndMice;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CatsAndMiceCommand implements CommandExecutor {
    
    private final CatsAndMice plugin;
    
    public CatsAndMiceCommand(CatsAndMice plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cUsage: /mice create");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Vérifier la permission de base
        if (!player.hasPermission("catsandmice.command")) {
            player.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande ! (catsandmice.command)");
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
    
    private void sendHelp(Player player) {
        player.sendMessage("§6=== §eAide de CatsAndMice §6===");
        player.sendMessage("§6/mice create §f- Crée un nouvel hologramme de jeu");
        player.sendMessage("§6/mice reload §f- Recharge la configuration");
    }
    
    private void handleCreateCommand(Player player) {
        if (!player.hasPermission("catsandmice.command.create")) {
            player.sendMessage("§cVous n'avez pas la permission de créer un hologramme ! (catsandmice.command.create)");
            return;
        }
        
        // Logique de création d'hologramme
        plugin.getGameManager().createHologram(player.getLocation());
        player.sendMessage("§aHologramme créé avec succès !");
    }
    
    private void handleReloadCommand(Player player) {
        if (!player.hasPermission("catsandmice.command.reload")) {
            player.sendMessage("§cVous n'avez pas la permission de recharger la configuration ! (catsandmice.command.reload)");
            return;
        }
        
        try {
            plugin.reloadConfig();
            plugin.getGameManager().reload();
            player.sendMessage("§aConfiguration rechargée avec succès !");
        } catch (Exception e) {
            player.sendMessage("§cErreur lors du rechargement de la configuration : " + e.getMessage());
            plugin.getLogger().severe("Erreur lors du rechargement de la configuration : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
