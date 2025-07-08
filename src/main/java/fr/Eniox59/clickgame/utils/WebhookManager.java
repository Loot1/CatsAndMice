package fr.Eniox59.clickgame.utils;

import com.google.gson.*;
import fr.Eniox59.clickgame.ClickGame;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class WebhookManager {
    private final ClickGame plugin;
    private final String webhookUrl;
    private final String username;
    private final String avatarUrl;
    private final boolean enabled;

    public WebhookManager(ClickGame plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        this.webhookUrl = config.getString("webhook.url", "");
        this.username = config.getString("webhook.username", "ClickGame");
        this.avatarUrl = config.getString("webhook.avatar-url", "");
        this.enabled = !webhookUrl.isEmpty();
    }

    public void sendScoreAlert(String playerName, int score) {
        if (!enabled || !plugin.getConfig().getBoolean("settings.score-alerts.enabled", true)) {
            return;
        }

        int threshold = plugin.getConfig().getInt("settings.score-alerts.threshold", 100);
        if (score < threshold) {
            return;
        }

        String message = plugin.getConfig().getString("settings.score-alerts.message", 
                "**%player%** a atteint un score de **%score%** !")
                .replace("%player%", playerName)
                .replace("%score%", String.valueOf(score));

        int color = plugin.getConfig().getInt("settings.score-alerts.color", 5814783);

        JsonObject embed = new JsonObject();
        embed.addProperty("title", "Score Élevé !");
        embed.addProperty("description", message);
        embed.addProperty("color", color);
        embed.addProperty("timestamp", new java.util.Date().toInstant().toString());

        sendWebhook(embed);
    }

    public void sendBestScore(String playerName, int score) {
        if (!enabled || !plugin.getConfig().getBoolean("webhook.best-score-notification", true)) {
            return;
        }

        String message = plugin.getConfig().getString("webhook.best-score-message", 
                "🎉 **NOUVEAU MEILLEUR SCORE** 🎉\n**%player%** a établi un nouveau record avec **%score%** points !")
                .replace("%player%", playerName)
                .replace("%score%", String.valueOf(score));

        JsonObject embed = new JsonObject();
        embed.addProperty("title", "Nouveau Meilleur Score !");
        embed.addProperty("description", message);
        embed.addProperty("color", 16753920); // Orange
        embed.addProperty("timestamp", new java.util.Date().toInstant().toString());

        sendWebhook(embed);
    }

    private void sendWebhook(JsonObject embed) {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = URI.create(webhookUrl).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "ClickGame-Webhook");
                connection.setDoOutput(true);

                JsonObject json = new JsonObject();
                json.addProperty("username", username);
                json.addProperty("avatar_url", avatarUrl);
                
                JsonArray embeds = new JsonArray();
                embeds.add(embed);
                json.add("embeds", embeds);

                String jsonString = new Gson().toJson(json);
                byte[] postData = jsonString.getBytes(StandardCharsets.UTF_8);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(postData);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode < 200 || responseCode > 299) {
                    plugin.getLogger().warning("Erreur lors de l'envoi du webhook: " + responseCode);
                }
                connection.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur lors de l'envoi du webhook: " + e.getMessage());
                if (plugin.getConfig().getBoolean("settings.debug.log-webhooks", false)) {
                    e.printStackTrace();
                }
            }
        });
    }
}
