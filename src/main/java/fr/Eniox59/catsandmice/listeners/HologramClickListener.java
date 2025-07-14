package fr.Eniox59.catsandmice.listeners;

import fr.Eniox59.catsandmice.CatsAndMice;
import fr.Eniox59.catsandmice.managers.GameManager;
import eu.decentsoftware.holograms.event.HologramClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class HologramClickListener implements Listener {

    private final CatsAndMice plugin;
    private final GameManager gameManager;

    public HologramClickListener(CatsAndMice plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onHologramClick(HologramClickEvent event) {
        try {
            // Vérifier que l'hologramme est bien celui du jeu
            if (event.getHologram() == null || !event.getHologram().getName().equalsIgnoreCase("catsandmice_hologram")) {
                return;
            }
            
            // Récupérer le joueur et annuler l'événement
            Player player = event.getPlayer();
            event.setCancelled(true);
            
            // Gérer le clic via le GameManager
            // Les permissions sont vérifiées dans le GameManager
            if (plugin.getConfig().getBoolean("settings.debug.log-clicks", false)) {
                plugin.getLogger().info("[DEBUG] Clic sur l'hologramme par " + player.getName());
            }
            gameManager.handleHologramClick(player);
            
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur lors du traitement du clic sur l'hologramme: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
