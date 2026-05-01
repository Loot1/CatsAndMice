package fr.loot1.catsandmice.listeners;

import fr.loot1.catsandmice.CatsAndMice;
import eu.decentsoftware.holograms.event.HologramClickEvent;
import fr.loot1.catsandmice.Click;
import fr.loot1.catsandmice.managers.ConfigManager;
import fr.loot1.catsandmice.managers.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class HologramClickListener implements Listener {

    private final ConfigManager configManager;
    private final GameManager gameManager;

    public HologramClickListener(CatsAndMice catsAndMice) {
        this.configManager = catsAndMice.getConfigManager();
        this.gameManager = catsAndMice.getGameManager();
    }

    @EventHandler
    public void onHologramClick(HologramClickEvent event) {
        if (event.getHologram().getName().equalsIgnoreCase(configManager.get("settings.hologram-name"))) {
            Player player = event.getPlayer();
            event.setCancelled(true);
            if (player.hasPermission("catsandmice.cat")) {
                Click lastClick = gameManager.getLastClick();
                if(lastClick == null || lastClick.getScore() != 0) {
                    gameManager.resetScore(player);
                } else {
                    player.sendMessage(configManager.getColored("messages.errors.already-reset"));
                }
            } else if(player.hasPermission("catsandmice.mice")) {
                if (player.hasPermission("catsandmice.bypass")) {
                    gameManager.addScore(player);
                } else {
                    long currentTime = System.currentTimeMillis();
                    Click lastClick = gameManager.getLastClick();
                    if(lastClick == null || lastClick.getScore() == 0) {
                        gameManager.addScore(player);
                    } else {
                        long lastClickTime = lastClick.getDate();
                        long clickDelay = configManager.getLong("settings.click-delay") * 1000L;
                        if (currentTime - lastClickTime < clickDelay) {
                            long timeLeft = (clickDelay - (currentTime - lastClickTime)) / 1000;
                            if (timeLeft > 0) {
                                player.sendMessage(configManager.getColoredReplaced("messages.errors.click-cooldown", new String[]{"time"}, new String[]{String.valueOf(timeLeft)}));
                            } else {
                                gameManager.addScore(player);
                            }
                        } else {
                            gameManager.addScore(player);
                        }
                    }
                }
            } else {
                player.sendMessage(configManager.getColored("messages.errors.permission-denied"));
            }
        }
    }

}
