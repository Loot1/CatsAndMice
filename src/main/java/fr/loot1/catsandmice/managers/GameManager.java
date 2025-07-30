package fr.loot1.catsandmice.managers;

import java.io.IOException;

import fr.loot1.catsandmice.CatsAndMice;
import fr.loot1.catsandmice.Click;
import fr.loot1.catsandmice.utils.DiscordWebhook;
import org.bukkit.entity.Player;

import java.util.*;

public class GameManager {
    
    private final CatsAndMice main;
    private final ConfigManager configManager;
    private final DataFileManager dataFileManager;
    private final HologramManager hologramManager;

    private final List<Click> clicks;
    private Click lastClick;
    private Click lastBestClick;

    public GameManager(CatsAndMice catsAndMice) {
        this.main = catsAndMice;
        this.configManager = catsAndMice.getConfigManager();
        this.dataFileManager = catsAndMice.getDataFileManager();
        this.hologramManager = new HologramManager(catsAndMice);

        this.clicks = dataFileManager.getClicks("clicks");
        this.lastClick = clicks.stream()
                .max(Comparator.comparingLong(Click::getDate))
                .orElse(null);
        this.lastBestClick = clicks.stream()
                .max(Comparator.comparingInt(Click::getScore))
                .orElse(null);

        hologramManager.init(getLastClicks());
    }

    public Click getLastClick() {
        return lastClick;
    }

    public List<Click> getLastClicks() {
        int lastClicksToShow = configManager.getInt("settings.last-clicks");
        return clicks.stream()
                .sorted(Comparator.comparingLong(Click::getDate).reversed())
                .limit(lastClicksToShow)
                .toList();
    }

    public void addScore(Player player) {
        long currentTime = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();
        int newClickCount = lastClick != null ? lastClick.getScore() + 1 : 1;
        Click newClick = new Click(playerId, currentTime, newClickCount);
        clicks.add(newClick);
        lastClick = newClick;
        dataFileManager.updateClicks("clicks", clicks);

        hologramManager.update(getLastClicks());
        checkScoreAlert(player);
        player.sendMessage(configManager.getColoredReplaced(
                "messages.success.clicked",
                new String[]{"score"}, new String[]{String.valueOf(newClickCount)})
        );
    }

    private void checkScoreAlert(Player player) {
        if (!configManager.getBoolean("webhook.enabled")) {
            return;
        }

        int threshold = configManager.getInt("webhook.threshold");

        if (lastClick.getScore() == threshold) {
            String[] placeholders = {"score", "player"};
            String[] values = {String.valueOf(lastClick.getScore()), player.getName()};
            if (configManager.getBoolean("webhook.console-message")) {
                main.getLogger().info(configManager.getColoredReplaced("webhook.console-alert", placeholders, values));
            }

            String webhookUrl = configManager.get("webhook.url");
            if (webhookUrl == null || !webhookUrl.startsWith("https://discord.com/api/webhooks/") || webhookUrl.contains("votre_webhook_ici")) {
                return;
            }

            String baseMessage = configManager.getColoredReplaced("webhook.alert-message", placeholders, values);

            final String message;
            String mention = configManager.get("webhook.mention");
            if (!mention.isEmpty()) {
                if (!mention.trim().isEmpty()) {
                    message = mention + "\n" + baseMessage;
                } else {
                    message = baseMessage;
                }
            } else {
                message = baseMessage;
            }

            if (configManager.getBoolean("webhook.enabled")) {
                main.getServer().getScheduler().runTaskAsynchronously(main, () -> {
                    try {
                        DiscordWebhook webhook = new DiscordWebhook(webhookUrl);
                        webhook.setContent(message);
                        webhook.execute();
                    } catch (IOException e) {
                        main.getLogger().warning("Erreur lors de l'envoi du webhook: " + e.getMessage());
                    }
                });
            }
        }
    }

    public void resetScore(Player player) {
        // todo: Ajouter un message de réinitialisation comme un clic spécial à la fin de la liste
        // todo: faire en sorte que le click ne se détecte pas sur l'hologramme mais bien sur le bouton

        long currentTime = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();
        String oldClickScore = lastClick != null ? String.valueOf(lastClick.getScore()) : "inconnu";
        Click newClick = new Click(playerId, currentTime, 0);
        clicks.add(newClick);
        lastClick = newClick;
        dataFileManager.updateClicks("clicks", clicks);

        int bestClickCount = lastBestClick != null ? lastBestClick.getScore() : 0;
        if (newClick.getScore() > bestClickCount) {
            lastBestClick = newClick;
            if (configManager.getBoolean("settings.notify-new-best-score")) {
                main.getServer().broadcastMessage(configManager.getColoredReplaced(
                        "messages.success.new-best-score",
                        new String[]{"score", "player"}, new String[]{String.valueOf(bestClickCount), player.getName()})
                );
            }
        }

        hologramManager.update(getLastClicks());

        player.sendMessage(configManager.getColoredReplaced(
                "messages.success.score-reset",
                new String[]{"score"}, new String[]{oldClickScore})
        );
    }

}
