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
    private int lastClicksToShow;

    public GameManager(CatsAndMice catsAndMice, HologramManager hologramManager) {
        this.main = catsAndMice;
        this.configManager = catsAndMice.getConfigManager();
        this.dataFileManager = catsAndMice.getDataFileManager();
        this.hologramManager = hologramManager;

        this.lastClicksToShow = configManager.getInt("settings.last-clicks");
        this.clicks = dataFileManager.getClicks("clicks");
        this.lastClick = clicks.stream()
                .max(Comparator.comparingLong(Click::getDate))
                .orElse(null);
        this.lastBestClick = clicks.stream()
                .filter(c -> c.getScore() > 0)
                .max(Comparator.comparingInt(Click::getScore))
                .orElse(null);

        hologramManager.init(getLastClicks(), lastBestClick);
    }

    public void refreshSettings() {
        this.lastClicksToShow = configManager.getInt("settings.last-clicks");
    }

    public Click getLastClick() {
        return lastClick;
    }

    public Click getLastBestClick() {
        return lastBestClick;
    }

    public List<Click> getLastClicks() {
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

        hologramManager.update(getLastClicks(), lastBestClick);
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

            String mention = configManager.get("webhook.mention");
            final String message = (mention != null && !mention.trim().isEmpty())
                    ? mention + "\n" + baseMessage
                    : baseMessage;

            main.getServer().getScheduler().runTaskAsynchronously(main, () -> {
                try {
                    DiscordWebhook webhook = new DiscordWebhook(webhookUrl);
                    webhook.setContent(message);
                    webhook.execute();
                } catch (IOException e) {
                    main.getLogger().warning("Error while sending webhook: " + e.getMessage());
                }
            });
        }
    }

    public void resetScore(Player player) {
        long currentTime = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();
        String oldClickScore = lastClick != null ? String.valueOf(lastClick.getScore()) : "unknown";

        int previousScore = lastClick != null ? lastClick.getScore() : 0;
        int bestClickCount = lastBestClick != null ? lastBestClick.getScore() : 0;
        if (previousScore > bestClickCount) {
            lastBestClick = lastClick;
            if (configManager.getBoolean("settings.notify-new-best-score")) {
                main.getServer().broadcastMessage(configManager.getColoredReplaced(
                        "messages.success.new-best-score",
                        new String[]{"score", "player"}, new String[]{String.valueOf(previousScore), player.getName()})
                );
            }
        }

        Click newClick = new Click(playerId, currentTime, 0);
        clicks.add(newClick);
        lastClick = newClick;
        dataFileManager.updateClicks("clicks", clicks);

        hologramManager.update(getLastClicks(), lastBestClick);

        player.sendMessage(configManager.getColoredReplaced(
                "messages.success.score-reset",
                new String[]{"score"}, new String[]{oldClickScore})
        );
    }

}

