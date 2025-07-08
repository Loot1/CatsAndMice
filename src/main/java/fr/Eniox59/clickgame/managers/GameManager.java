package fr.Eniox59.clickgame.managers;

import com.google.gson.JsonObject;
import fr.Eniox59.clickgame.ClickGame;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.io.File;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;

public class GameManager {
    
    private final ClickGame plugin;
    private int currentScore;
    private int bestScore;
    private String bestPlayerName;
    private final Map<UUID, Integer> playerClicks;
    private final Map<UUID, Long> lastClickTimes;
    private File dataFile;
    private FileConfiguration dataConfig;
    
    public GameManager(ClickGame plugin) {
        this.plugin = plugin;
        this.playerClicks = new HashMap<>();
        this.lastClickTimes = new HashMap<>();
        this.currentScore = 0;
        
        // Chargement de la configuration
        loadData();
        
        // Charger la position de l'hologramme si elle existe
        loadHologramLocation();
        
        // Initialisation des composants
    }
    
    public void handleHologramClick(Player player) {
        try {
            // Vérifier si le joueur a la permission de réinitialiser
            if (player.hasPermission("clickgame.reset") && player.isSneaking()) {
                if (plugin.getConfig().getBoolean("settings.debug.log-clicks", false)) {
                    plugin.getLogger().info(String.format("[DEBUG] %s a demandé une réinitialisation du score", player.getName()));
                }
                resetScore(player);
                return;
            }
            
            // Vérifier si le joueur doit attendre avant de cliquer à nouveau
            if (!canClick(player)) {
                if (plugin.getConfig().getBoolean("settings.debug.log-clicks", false)) {
                    plugin.getLogger().info(String.format("[DEBUG] %s doit attendre avant de pouvoir cliquer à nouveau", player.getName()));
                }
                return;
            }
            
            // Ajouter un point
            addClick(player);
            player.sendMessage("§a+1 point ! Score actuel : §e" + currentScore);
            
            // Mettre à jour le temps du dernier clic
            lastClickTimes.put(player.getUniqueId(), System.currentTimeMillis());
            
            // Log du clic si le mode debug est activé
            if (plugin.getConfig().getBoolean("settings.debug.log-clicks", false)) {
                plugin.getLogger().info(String.format("[CLIC] %s a cliqué - Score: %d (Meilleur: %d par %s)", 
                    player.getName(), 
                    playerClicks.getOrDefault(player.getUniqueId(), 0),
                    bestScore,
                    bestPlayerName != null ? bestPlayerName : "Personne"));
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur lors du clic sur l'hologramme: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§cUne erreur est survenue lors du traitement de votre action.");
        }
    }
    
    public void addClick(Player player) {
        try {
            // Incrémenter le score
            currentScore++;
            
            // Vérifier si c'est un nouveau record
            if (currentScore > bestScore) {
                int previousBest = bestScore;
                String previousBestPlayer = bestPlayerName;
                
                bestScore = currentScore;
                bestPlayerName = player.getName();
                saveData();
                
                // Envoyer la notification de record à tous les joueurs
                if (plugin.getConfig().getBoolean("settings.debug.log-records", true)) {
                    String recordMessage = ChatColor.translateAlternateColorCodes('&', 
                        plugin.getConfig().getString("messages.new-best-score", "&6&lNouveau meilleur score : &e%score% &6par &a%player%&6!")
                            .replace("%player%", player.getName())
                            .replace("%score%", String.valueOf(bestScore))
                            .replace("%previous_score%", String.valueOf(previousBest))
                            .replace("%previous_player%", previousBestPlayer != null ? previousBestPlayer : "Personne")
                    );
                    
                    // Envoyer le message à tous les joueurs en ligne
                    for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                        onlinePlayer.sendMessage(recordMessage);
                    }
                    
                    if (plugin.getConfig().getBoolean("settings.debug.log-records", false)) {
                        plugin.getLogger().info(String.format("[RECORD] %s a etabli un nouveau record: %d points (precedent: %d par %s)",
                            player.getName(), bestScore, previousBest, previousBestPlayer != null ? previousBestPlayer : "Personne"));
                    }
                }
            }
            
            // Vérifier si on doit déclencher une alerte de score
            checkScoreAlert(player);
            
            // Mettre à jour l'hologramme
            updateHologram();
            
            // Ajouter le clic au compteur du joueur
            playerClicks.merge(player.getUniqueId(), 1, Integer::sum);
            
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur lors de l'ajout d'un clic: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§cUne erreur est survenue lors du traitement de votre action.");
        }
    }
    
    /**
     * Vérifie si une alerte de score doit être déclenchée
     * @param player Le joueur qui a marqué le point
     */
    private void checkScoreAlert(Player player) {
        try {
            // Vérifier si le webhook est activé
            if (!plugin.getConfig().getBoolean("webhook.enabled", false)) {
                if (plugin.getConfig().getBoolean("settings.debug.log-records", false)) {
                    plugin.getLogger().info("Webhook désactivé dans la configuration");
                }
                return;
            }
            
            // Vérifier si le score atteint le seuil
            int threshold = plugin.getConfig().getInt("webhook.threshold", 100);
            if (currentScore == threshold) {
                String webhookUrl = plugin.getConfig().getString("webhook.url");
                
                // Vérification plus stricte de l'URL
                if (webhookUrl == null || webhookUrl.isEmpty() || 
                    !webhookUrl.startsWith("https://discord.com/api/webhooks/") || 
                    webhookUrl.contains("votre_webhook_ici")) {
                    plugin.getLogger().warning("URL du webhook non configurée ou invalide: " + 
                        (webhookUrl != null && !webhookUrl.isEmpty() ? "(URL masquée pour des raisons de sécurité)" : "null/empty"));
                    return;
                }
                
                // Récupération du message avec une valeur par défaut plus claire
                String defaultMessage = "**🎮 CLICKGAME - NOUVEAU RECORD !**\n" +
                                     "━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                     "🏆 **Joueur:** %player%\n" +
                                     "⚡ **Score atteint:** %score%\n" +
                                     "━━━━━━━━━━━━━━━━━━━━━━━━━━━";
                
                String message = plugin.getConfig().getString("webhook.alert-message", defaultMessage)
                        .replace("%player%", player.getName())
                        .replace("%score%", String.valueOf(currentScore));
                
                // Ajouter la mention si activée
                if (plugin.getConfig().getBoolean("webhook.mention-enabled", false)) {
                    String mention = plugin.getConfig().getString("webhook.mention", "");
                    if (mention != null && !mention.trim().isEmpty()) {
                        message = mention + "\n" + message;
                        if (plugin.getConfig().getBoolean("settings.debug.log-records", false)) {
                            plugin.getLogger().info("Mention ajoutée au webhook: " + mention);
                        }
                    }
                }
                
                // Log de débogage
                if (plugin.getConfig().getBoolean("settings.debug.log-records", false)) {
                    plugin.getLogger().info("Envoi du webhook à: " + 
                        webhookUrl.substring(0, Math.min(webhookUrl.length(), 30)) + "...");
                    plugin.getLogger().info("Message à envoyer: " + message);
                }
                
                // Envoyer le webhook de manière asynchrone
                final String finalMessage = message;
                new Thread(() -> sendDiscordWebhook(webhookUrl, finalMessage)).start();
                
                // Log dans la console
                plugin.getLogger().info(String.format("Alerte de score envoyée via webhook: %s a atteint %d points", 
                    player.getName(), currentScore));
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur lors de l'envoi de l'alerte de score: " + e.getMessage());
            if (plugin.getConfig().getBoolean("settings.debug.log-hologram-update", false)) {
                e.printStackTrace();
            }
        }
    }
    
    public void resetScore(Player player) {
        try {
            // Vérifier la permission
            if (player != null && !player.hasPermission("clickgame.reset")) {
                player.sendMessage("§cVous n'avez pas la permission de réinitialiser le score !");
                return;
            }
            
            int previousBest = bestScore;
            String previousBestPlayer = bestPlayerName;
            
            // Réinitialiser le score actuel
            currentScore = 0;
            
            // Mettre à jour l'hologramme
            updateHologram();
            
            // Sauvegarder les modifications
            saveData();
            
            // Message de confirmation
            if (player != null) {
                player.sendMessage("§aLe score a été réinitialisé !");
            }
            
            // Log de la reinitialisation
            if (plugin.getConfig().getBoolean("settings.debug.log-records", false)) {
                plugin.getLogger().info(String.format(
                    "[SCORE] %s a reinitialise le score (Ancien meilleur: %d par %s)", 
                    player != null ? player.getName() : "Console",
                    previousBest,
                    previousBestPlayer != null ? previousBestPlayer : "Personne"
                ));
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur lors de la réinitialisation du score: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Vérifie si un joueur peut cliquer à nouveau
     * @param player Le joueur à vérifier
     * @return true si le joueur peut cliquer, false sinon
     */
    private boolean canClick(Player player) {
        // Seuls les joueurs avec la permission clickgame.bypass spécifique n'ont pas de délai
        // Les opérateurs n'ont plus de bypass automatique
        if (player.hasPermission("clickgame.bypass")) {
            return true;
        }
        
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastClick = lastClickTimes.getOrDefault(playerId, 0L);
        long clickDelay = plugin.getConfig().getLong("settings.click-delay", 300) * 1000; // en millisecondes
        
        // Vérifier si le délai est écoulé
        if (currentTime - lastClick < clickDelay) {
            long remainingTime = ((lastClick + clickDelay) - currentTime) / 1000; // en secondes
            String waitMessage = plugin.getConfig().getString("settings.wait-message", "§6Veuillez patienter §e%time% §6secondes avant de pouvoir cliquer à nouveau.")
                    .replace("%time%", String.valueOf(remainingTime));
            player.sendMessage(waitMessage);
            return false;
        }
        
        return true;
    }
    
    public void updateHologram() {
        try {
            // Récupérer le nom de l'hologramme depuis la configuration
            String hologramName = plugin.getConfig().getString("hologram.name", "clickgame_hologram");
            
            // Récupérer l'hologramme existant
            Hologram hologram = DHAPI.getHologram(hologramName);
            if (hologram == null) {
                plugin.getLogger().warning("L'hologramme '" + hologramName + "' n'existe pas");
                return;
            }
            
            List<String> lines = new ArrayList<>();
            
            // Récupérer les couleurs depuis la config
            String textColor = "&f";
            String scoreColor = "&e";
            String bestScoreColor = "&6&l";
            
            if (plugin.getConfig().getBoolean("hologram.style.use-custom-colors", true)) {
                textColor = plugin.getConfig().getString("hologram.style.text-color", "&f");
                scoreColor = plugin.getConfig().getString("hologram.style.score-color", "&e");
                bestScoreColor = plugin.getConfig().getString("hologram.style.best-score-color", "&6&l");
            }
            
            // Ajouter l'en-tête avec les couleurs
            List<String> headerLines = plugin.getConfig().getStringList("hologram.header");
            for (String line : headerLines) {
                lines.add(ChatColor.translateAlternateColorCodes('&', line.replace("%text-color%", textColor)));
            }
            
            // Ajouter le score actuel avec la couleur du score
            lines.add(ChatColor.translateAlternateColorCodes('&', 
                String.format("%sScore actuel: %s%d", 
                    textColor.replace("&", "§"), 
                    scoreColor.replace("&", "§"), 
                    currentScore)));

            // Ajouter le meilleur score avec la couleur du meilleur score
            String bestScoreLine = String.format("%sMeilleur score: %s%d", 
                textColor.replace("&", "§"), 
                bestScoreColor.replace("&", "§"), 
                bestScore);

            if (bestPlayerName != null) {
                bestScoreLine += String.format(" %spar %s%s", 
                    textColor.replace("&", "§"), 
                    bestScoreColor.replace("&", "§"), 
                    bestPlayerName);
            }
            lines.add(bestScoreLine);

            // Ajouter le bouton avec les couleurs
            List<String> buttonLines = plugin.getConfig().getStringList("hologram.button");
            lines.addAll(buttonLines);

            // Mettre à jour l'hologramme
            eu.decentsoftware.holograms.api.DHAPI.setHologramLines(hologram, lines);

            // Log de mise à jour si activé
            if (plugin.getConfig().getBoolean("settings.debug.log-hologram-update", false)) {
                plugin.getLogger().info("Hologramme mis a jour - Score: " + currentScore + ", Meilleur: " + bestScore);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur lors de la mise à jour de l'hologramme: " + e.getMessage());
            if (plugin.getConfig().getBoolean("settings.debug.log-hologram-update", false)) {
                e.printStackTrace();
            }
        }
    }

    private void loadData() {
        try {
            // Créer le fichier de données s'il n'existe pas
            dataFile = new File(plugin.getDataFolder(), "data.yml");
            if (!dataFile.exists()) {
                plugin.saveResource("data.yml", false);
                if (plugin.getConfig().getBoolean("settings.debug.log-hologram-update", false)) {
                    plugin.getLogger().info("Fichier de donnees cree");
                }
            }
            
            // Charger la configuration
            dataConfig = YamlConfiguration.loadConfiguration(dataFile);
            
            // Charger les données de base
            currentScore = dataConfig.getInt("current-score", 0);
            bestScore = dataConfig.getInt("best-score", 0);
            bestPlayerName = dataConfig.getString("best-player", null);
            
            // Charger les clics des joueurs
            if (dataConfig.contains("player-clicks")) {
                ConfigurationSection clicksSection = dataConfig.getConfigurationSection("player-clicks");
                if (clicksSection != null) {
                    for (String uuidStr : clicksSection.getKeys(false)) {
                        try {
                            UUID uuid = UUID.fromString(uuidStr);
                            int clicks = clicksSection.getInt(uuidStr);
                            playerClicks.put(uuid, clicks);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("UUID invalide dans les donnees: " + uuidStr);
                        }
                    }
                }
            }
            
            // Charger les temps de clic
            if (dataConfig.contains("last-click-times")) {
                ConfigurationSection timesSection = dataConfig.getConfigurationSection("last-click-times");
                if (timesSection != null) {
                    for (String uuidStr : timesSection.getKeys(false)) {
                        try {
                            UUID uuid = UUID.fromString(uuidStr);
                            long lastClick = timesSection.getLong(uuidStr);
                            lastClickTimes.put(uuid, lastClick);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("UUID invalide dans les temps de clic: " + uuidStr);
                        }
                    }
                }
            }
            
            if (plugin.getConfig().getBoolean("settings.debug.log-records", false)) {
                plugin.getLogger().info("Donnees chargees");
                plugin.getLogger().info("Meilleur score: " + bestScore + " par " + 
                    (bestPlayerName != null ? bestPlayerName : "Personne"));
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur lors du chargement: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void saveData() {
        try {
            if (dataFile == null) {
                dataFile = new File(plugin.getDataFolder(), "data.yml");
            }
            
            if (dataConfig == null) {
                dataConfig = new YamlConfiguration();
            }
            
            // Sauvegarder les scores
            dataConfig.set("current-score", currentScore);
            dataConfig.set("best-score", bestScore);
            dataConfig.set("best-player", bestPlayerName);
            
            // Sauvegarder les clics des joueurs
            ConfigurationSection clicksSection = dataConfig.createSection("player-clicks");
            for (Map.Entry<UUID, Integer> entry : playerClicks.entrySet()) {
                clicksSection.set(entry.getKey().toString(), entry.getValue());
            }
            
            // Sauvegarder les temps de clic
            ConfigurationSection timesSection = dataConfig.createSection("last-click-times");
            for (Map.Entry<UUID, Long> entry : lastClickTimes.entrySet()) {
                timesSection.set(entry.getKey().toString(), entry.getValue());
            }
            
            // Sauvegarder le fichier
            dataConfig.save(dataFile);
            
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur lors de la sauvegarde: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public int getCurrentScore() {
        return currentScore;
    }
    
    public int getBestScore() {
        return bestScore;
    }
    
    /**
     * Envoie une requête au webhook Discord
     * @param webhookUrl L'URL du webhook Discord
     * @param message Le message à envoyer
     */
    private void sendDiscordWebhook(String webhookUrl, String message) {
        HttpURLConnection connection = null;
        try {
            // Créer le payload JSON avec Gson
            JsonObject json = new JsonObject();
            json.addProperty("content", message);
            
            // Créer la requête HTTP avec URI pour éviter la dépréciation
            URI uri = new URI(webhookUrl);
            URL url = uri.toURL();
            
            if (plugin.getConfig().getBoolean("settings.debug.log-records", false)) {
                plugin.getLogger().info("Tentative de connexion au webhook...");
            }
            
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("User-Agent", "ClickGame-Plugin/1.0");
            connection.setConnectTimeout(10000); // 10 secondes de timeout
            connection.setReadTimeout(10000);    // 10 secondes de timeout
            connection.setDoOutput(true);
            
            // Envoyer les données
            try (OutputStream os = connection.getOutputStream()) {
                String jsonPayload = json.toString();
                if (plugin.getConfig().getBoolean("settings.debug.log-records", false)) {
                    plugin.getLogger().info("Envoi du payload JSON: " + jsonPayload);
                }
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush();
            } catch (IOException e) {
                plugin.getLogger().severe("Erreur lors de l'envoi des données du webhook: " + e.getMessage());
                return;
            }
            
            // Vérifier la réponse
            int responseCode = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();
            
            if (plugin.getConfig().getBoolean("settings.debug.log-records", false)) {
                plugin.getLogger().info("Réponse du serveur: " + responseCode + " " + responseMessage);
            }
            
            // Lire la réponse, qu'il y ait une erreur ou non
            try (InputStream inputStream = responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream()) {
                if (inputStream != null) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        
                        if (responseCode >= 200 && responseCode < 300) {
                            plugin.getLogger().info("Webhook envoyé avec succès");
                            if (plugin.getConfig().getBoolean("settings.debug.log-records", false)) {
                                plugin.getLogger().info("Réponse complète: " + response);
                            }
                        } else {
                            plugin.getLogger().warning("Erreur lors de l'envoi du webhook: " + 
                                responseCode + " " + responseMessage + " - " + response);
                        }
                    }
                } else if (responseCode >= 400) {
                    plugin.getLogger().warning("Erreur lors de l'envoi du webhook: " + 
                        responseCode + " " + responseMessage + " (pas de détail supplémentaire)");
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur lors de l'envoi du webhook: " + e.getMessage());
            if (plugin.getConfig().getBoolean("settings.debug.log-records", false)) {
                e.printStackTrace();
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Crée ou met à jour un hologramme à l'emplacement spécifié
     * @param location L'emplacement où créer l'hologramme
     */
    public void createHologram(Location location) {
        try {
            // Récupérer le nom de l'hologramme depuis la configuration
            String hologramName = plugin.getConfig().getString("hologram.name", "clickgame_hologram");
            
            // Vérifier si l'hologramme existe déjà
            Hologram existingHologram = DHAPI.getHologram(hologramName);
            
            if (existingHologram != null) {
                // Mettre à jour la position si l'hologramme existe déjà
                DHAPI.moveHologram(existingHologram, location);
                if (plugin.getConfig().getBoolean("settings.debug.log-hologram-update", false)) {
                    plugin.getLogger().info("Hologramme deplace avec succes !");
                }
            } else {
                // Créer un nouvel hologramme vide
                List<String> lines = new ArrayList<>();
                lines.add("&6&lCLICK GAME");
                lines.add("&7Score: &e0");
                lines.add("&7Meilleur score: &60");
                lines.add("&e&l[CLIQUEZ POUR JOUER]");
                
                Hologram hologram = DHAPI.createHologram(hologramName, location, false, lines);
                
                if (hologram != null) {
                    if (plugin.getConfig().getBoolean("settings.debug.log-hologram-update", false)) {
                        plugin.getLogger().info("Hologramme cree avec succes !");
                    }
                    // Mettre à jour l'hologramme avec les valeurs actuelles
                    updateHologram();
                } else {
                    throw new Exception("Erreur lors de la creation de l'hologramme");
                }
            }
            
            // Sauvegarder la position dans la configuration
            plugin.getConfig().set("hologram.location.world", location.getWorld().getName());
            plugin.getConfig().set("hologram.location.x", location.getX());
            plugin.getConfig().set("hologram.location.y", location.getY());
            plugin.getConfig().set("hologram.location.z", location.getZ());
            plugin.getConfig().set("hologram.location.yaw", (double)location.getYaw());
            plugin.getConfig().set("hologram.location.pitch", (double)location.getPitch());
            plugin.saveConfig();
            
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur lors de la creation de l'hologramme: " + e.getMessage());
            if (plugin.getConfig().getBoolean("settings.debug.log-hologram-update", false)) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Charge la position de l'hologramme depuis la configuration et le crée s'il n'existe pas
     */
    private void loadHologramLocation() {
        try {
            // Vérifier si une position est enregistrée dans la configuration
            if (plugin.getConfig().isSet("hologram.location.world")) {
                String worldName = plugin.getConfig().getString("hologram.location.world");
                double x = plugin.getConfig().getDouble("hologram.location.x");
                double y = plugin.getConfig().getDouble("hologram.location.y");
                double z = plugin.getConfig().getDouble("hologram.location.z");
                float yaw = (float)plugin.getConfig().getDouble("hologram.location.yaw", 0.0);
                float pitch = (float)plugin.getConfig().getDouble("hologram.location.pitch", 0.0);
                
                // Vérifier que le monde existe
                if (plugin.getServer().getWorld(worldName) != null) {
                    Location location = new Location(
                        plugin.getServer().getWorld(worldName),
                        x, y, z, yaw, pitch
                    );
                    
                    // Créer l'hologramme à la position enregistrée
                    createHologram(location);
                    
                    if (plugin.getConfig().getBoolean("settings.debug.log-hologram-update", false)) {
                        plugin.getLogger().info(
                            "[HOLOGRAM] Chargement de la position: " + worldName + 
                            " X: " + (int)x + 
                            " Y: " + (int)y + 
                            " Z: " + (int)z
                        );
                    }
                } else {
                    plugin.getLogger().warning("Monde non trouvé pour l'hologramme: " + worldName);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors du chargement de la position de l'hologramme: " + e.getMessage());
            plugin.getLogger().severe("ERREUR: Impossible de creer ou mettre a jour l'hologramme !");
            plugin.getLogger().severe("Cause : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
