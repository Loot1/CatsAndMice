package fr.Eniox59.clickgame.managers;

import fr.Eniox59.clickgame.ClickGame;
import fr.Eniox59.clickgame.utils.DiscordWebhook;

import java.io.IOException;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Random;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;

public class GameManager {
    
    private final ClickGame plugin;
    private int currentScore;
    private int bestScore;
    private String bestPlayerName;
    // Stockage des clics des joueurs
    private final Map<UUID, Integer> playerClicks;
    private final Map<UUID, Long> lastClickTimes;
    // Stocke l'UUID du joueur, le timestamp et le score au moment du clic
    private final List<Map.Entry<UUID, Map.Entry<Long, Integer>>> clickHistory;
    private final Map<String, String> cachedMessages;
    private File dataFile;
    private FileConfiguration dataConfig;
    private final ExecutorService threadPool;
    private long lastHologramUpdate = 0;
    private Hologram hologram;
    
    /**
     * Charge les données du jeu depuis le fichier de données
     */
    private void loadData() {
        // Initialiser le fichier de données
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        // Charger les données
        bestScore = dataConfig.getInt("best-score", 0);
        bestPlayerName = dataConfig.getString("best-player", null);
        
        // Charger les clics des joueurs
        ConfigurationSection playerClicksSection = dataConfig.getConfigurationSection("player-clicks");
        if (playerClicksSection != null) {
            for (String uuidStr : playerClicksSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    int clicks = playerClicksSection.getInt(uuidStr);
                    playerClicks.put(uuid, clicks);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("UUID invalide dans les données des clics: " + uuidStr);
                }
            }
        }
        
        // Charger les derniers temps de clic
        ConfigurationSection lastClickTimesSection = dataConfig.getConfigurationSection("last-click-times");
        if (lastClickTimesSection != null) {
            for (String uuidStr : lastClickTimesSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    long lastClickTime = lastClickTimesSection.getLong(uuidStr);
                    lastClickTimes.put(uuid, lastClickTime);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("UUID invalide dans les temps de clic: " + uuidStr);
                }
            }
        }
        
        if (plugin.getConfig().getBoolean("settings.debug.log-load", false)) {
            plugin.getLogger().info("Données chargées - Meilleur score: " + bestScore + 
                               " par " + (bestPlayerName != null ? bestPlayerName : "Personne"));
        }
    }
    
    public GameManager(ClickGame plugin) {
        this.plugin = plugin;
        this.playerClicks = new HashMap<>();
        this.lastClickTimes = new HashMap<>();
        this.clickHistory = new ArrayList<>();
        this.cachedMessages = new HashMap<>();
        this.currentScore = 0;
        
        // Initialisation du pool de threads si activé
        if (plugin.getConfig().getBoolean("optimization.enable-thread-pool", true)) {
            int poolSize = plugin.getConfig().getInt("optimization.max-thread-pool-size", 10);
            this.threadPool = Executors.newFixedThreadPool(poolSize);
            plugin.getLogger().info("Thread pool initialized with " + poolSize + " threads");
        } else {
            this.threadPool = null;
        }
        
        // Chargement de la configuration
        loadData();
        
        // Charger les messages en cache si activé
        if (plugin.getConfig().getBoolean("optimization.enable-message-caching", true)) {
            loadMessages();
        }
        
        // Charger la position de l'hologramme si configuré
        if (plugin.getConfig().getBoolean("hologram.enabled", true)) {
            loadHologramLocation();
        }
    }
    
    /**
     * Vérifie si un joueur peut cliquer sur l'hologramme
     * @param player Le joueur qui tente de cliquer
     * @return true si le joueur peut cliquer, false sinon
     */
    private boolean canClick(Player player) {
        if (player == null) return false;
        
        // Vérifier si le joueur a la permission de contourner le délai
        if (player.hasPermission("clickgame.bypass")) {
            return true;
        }
        
        // Vérifier le délai entre les clics
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastClickTime = lastClickTimes.getOrDefault(playerId, 0L);
        // Convertir les secondes en millisecondes (1 seconde = 1000 ms)
        long clickDelay = plugin.getConfig().getLong("settings.click-delay", 1) * 1000L; // Délai par défaut de 1 seconde
        
        if (currentTime - lastClickTime < clickDelay) {
            // Le joueur doit encore attendre
            long timeLeft = (clickDelay - (currentTime - lastClickTime)) / 1000; // Convertir en secondes
            if (timeLeft > 0) {
                player.sendMessage(String.format("§cVous devez attendre encore %d secondes avant de pouvoir cliquer à nouveau !", timeLeft));
            } else {
                player.sendMessage("§cVeuillez patienter avant de cliquer à nouveau !");
            }
            return false;
        }
        
        return true;
    }
    
    public void handleHologramClick(Player player) {
        try {
            // Vérifier si le joueur a la permission de réinitialiser le score
            if (player.hasPermission("clickgame.reset")) {
                if (plugin.getConfig().getBoolean("settings.debug.log-clicks", false)) {
                    plugin.getLogger().info(String.format("[DEBUG] %s a demandé une réinitialisation du score", player.getName()));
                }
                resetScore(player);
                return;
            }
            
            // Vérifier si le joueur doit attendre avant de cliquer à nouveau
            if (!canClick(player)) {
                return;
            }
            
            // Mettre à jour le temps du dernier clic avant d'ajouter le point
            lastClickTimes.put(player.getUniqueId(), System.currentTimeMillis());
            
            // Ajouter un point
            addClick(player);
            player.sendMessage("§a+1 point ! Score actuel : §e" + currentScore);
            
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
            UUID playerId = player.getUniqueId();
            long currentTime = System.currentTimeMillis();
            
            // Mettre à jour le compteur de clics
            int newClickCount = playerClicks.getOrDefault(playerId, 0) + 1;
            playerClicks.put(playerId, newClickCount);
            lastClickTimes.put(playerId, currentTime);
            
            // Enregistrer ce clic dans l'historique avec le score au moment du clic
            int scoreAtClick = newClickCount;
            Map.Entry<Long, Integer> timeAndScore = new AbstractMap.SimpleEntry<>(currentTime, scoreAtClick);
            // Ajouter à la fin de la liste pour un affichage chronologique
            clickHistory.add(new AbstractMap.SimpleEntry<>(playerId, timeAndScore));
            
            // Log de débogage
            if (plugin.getConfig().getBoolean("settings.debug.log-clicks", false)) {
                plugin.getLogger().info(String.format("[DEBUG] Ajout d'un clic - Joueur: %s, Score: %d, Taille de l'historique: %d", 
                    player.getName(), scoreAtClick, clickHistory.size()));
            }
            
            // Limiter la taille de l'historique
            while (clickHistory.size() > 1000) { // Garder un maximum de 1000 entrées
                clickHistory.remove(clickHistory.size() - 1);
            }
            
            currentScore++;
            
            // Log de débogage
            if (plugin.getConfig().getBoolean("settings.debug.log-clicks", false)) {
                plugin.getLogger().info(String.format("[DEBUG] Score actuel: %d, Meilleur score: %d par %s", 
                    currentScore, bestScore, bestPlayerName != null ? bestPlayerName : "Personne"));
            }
            
            // Vérifier si c'est un nouveau record
            if (currentScore > bestScore) {
                bestScore = currentScore;
                bestPlayerName = player.getName();
                saveData();
                
                // Envoyer une notification si activé
                if (plugin.getConfig().getBoolean("settings.notify-new-best-score", true) && 
                    plugin.getConfig().getBoolean("settings.debug.log-records", false)) {
                    String message = getMessage("new-best-score", "&6Nouveau meilleur score: &e%score% &6par &a%player%&6!")
                        .replace("%score%", String.valueOf(bestScore))
                        .replace("%player%", bestPlayerName);
                    
                    // Envoyer à tous les joueurs
                    for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                        onlinePlayer.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                    }
                }
            }
            
            // Mettre à jour l'hologramme
            updateHologram();
            
            // Vérifier si une alerte de score doit être déclenchée
            checkScoreAlert(player);
            
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
            boolean debugLogs = plugin.getConfig().getBoolean("settings.debug.log-records", false);
            
            // Vérifier si le webhook est activé
            if (!plugin.getConfig().getBoolean("webhook.enabled", false)) {
                if (debugLogs) {
                    plugin.getLogger().info("Webhook désactivé dans la configuration");
                }
                return;
            }
            
            // Vérifier si le score atteint le seuil
            int threshold = plugin.getConfig().getInt("webhook.threshold", 100);
            if (currentScore == threshold) {
                // Afficher le message dans la console si activé
                if (plugin.getConfig().getBoolean("webhook.console-message", true)) {
                    String consoleMessage = plugin.getConfig().getString("webhook.console-alert", 
                        "&6[ClickGame] &aAlerte de score: %player% a atteint %score% points")
                        .replace("%player%", player.getName())
                        .replace("%score%", String.valueOf(currentScore));
                    plugin.getLogger().info(consoleMessage.replace("&", "§"));
                }
                
                // Vérifier si l'URL du webhook est valide
                String webhookUrl = plugin.getConfig().getString("webhook.url");
                if (webhookUrl == null || webhookUrl.isEmpty() || 
                    !webhookUrl.startsWith("https://discord.com/api/webhooks/") || 
                    webhookUrl.contains("votre_webhook_ici")) {
                        if (debugLogs) {
                            plugin.getLogger().warning("URL du webhook non configurée ou invalide: " + 
                                (webhookUrl != null && !webhookUrl.isEmpty() ? 
                                "(URL masquée pour des raisons de sécurité)" : "null/empty"));
                        }
                        return;
                }
                
                // Récupération du message avec une valeur par défaut
                String defaultMessage = "**🎮 CLICKGAME - NOUVEAU RECORD !**\n" +
                                     "━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                     "🏆 **Joueur:** %player%\n" +
                                     "⚡ **Score atteint:** %score%\n" +
                                     "━━━━━━━━━━━━━━━━━━━━━━━━━━━";
                
                // Construire le message de base
                String baseMessage = plugin.getConfig().getString("webhook.alert-message", defaultMessage)
                        .replace("%player%", player.getName())
                        .replace("%score%", String.valueOf(currentScore));
                
                // Construire le message final avec mention si nécessaire
                final String message;
                if (plugin.getConfig().getBoolean("webhook.mention-enabled", false)) {
                    String mention = plugin.getConfig().getString("webhook.mention", "");
                    if (mention != null && !mention.trim().isEmpty()) {
                        message = mention + "\n" + baseMessage;
                        if (debugLogs) {
                            plugin.getLogger().info("Mention ajoutée au webhook: " + mention);
                        }
                    } else {
                        message = baseMessage;
                    }
                } else {
                    message = baseMessage;
                }
                
                // Log de débogage
                if (debugLogs) {
                    String logMessage = String.format("Envoi du webhook pour le score de %s: %d points", 
                        player.getName(), currentScore);
                    plugin.getLogger().info("[WEBHOOK] " + logMessage);
                }
                
                // Envoi du webhook dans un thread séparé pour ne pas bloquer le thread principal
                if (plugin.getConfig().getBoolean("webhook.enabled", false)) {
                    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                        try {
                            DiscordWebhook webhook = new DiscordWebhook(webhookUrl);
                            webhook.setContent(message);
                            webhook.execute();
                            if (debugLogs) {
                                plugin.getLogger().info("[WEBHOOK] Message envoyé avec succès à Discord");
                            }
                        } catch (IOException e) {
                            plugin.getLogger().warning("Erreur lors de l'envoi du webhook: " + e.getMessage());
                            if (debugLogs) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur lors de la vérification de l'alerte de score: " + e.getMessage());
            if (plugin.getConfig().getBoolean("settings.debug.log-errors", true)) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Réinitialise le score actuel du jeu
     * @param player Le joueur qui demande la réinitialisation (peut être null pour la console)
     */
    public void resetScore(Player player) {
        try {
            // Vérifier la permission
            if (player != null && !player.hasPermission("clickgame.reset")) {
                player.sendMessage("§cVous n'avez pas la permission de réinitialiser le score !");
                return;
            }
            
            // Utiliser la nouvelle méthode resetGame qui gère l'affichage dans l'hologramme
            if (player != null) {
                resetGame(player);
                player.sendMessage("§aLe score a été réinitialisé !");
            }
            
            // Log de la réinitialisation
            if (plugin.getConfig().getBoolean("settings.debug.log-records", false)) {
                plugin.getLogger().info(String.format(
                    "[SCORE] %s a réinitialisé le score (Ancien meilleur: %d par %s)", 
                    player != null ? player.getName() : "Console",
                    bestScore,
                    bestPlayerName != null ? bestPlayerName : "Personne"
                ));
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur lors de la réinitialisation du score: " + e.getMessage());
            if (plugin.getConfig().getBoolean("settings.debug.log-errors", true)) {
                e.printStackTrace();
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
                
                // Supprimer l'ancien hologramme s'il existe
                if (this.hologram != null) {
                    this.hologram.delete();
                }
                
                // Créer le nouvel hologramme
                this.hologram = DHAPI.createHologram(hologramName, location, false, lines);
                
                if (this.hologram != null) {
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
     * Charge la position de l'hologramme depuis la configuration et le crée s'il n'existe pas.
     * La position est chargée depuis le fichier de configuration du plugin.
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
            if (plugin.getConfig().getBoolean("settings.debug.log-hologram-update", false)) {
                e.printStackTrace();
            }
        }
    }
    

    
    /**
     * Charge les messages en cache
     */
    private void loadMessages() {
        ConfigurationSection messagesSection = plugin.getConfig().getConfigurationSection("messages");
        if (messagesSection != null) {
            cachedMessages.clear();
            for (String key : messagesSection.getKeys(true)) {
                if (messagesSection.isString(key)) {
                    cachedMessages.put(
                        key,
                        ChatColor.translateAlternateColorCodes('&', messagesSection.getString(key))
                    );
                }
            }
            plugin.getLogger().info("Messages en cache chargés : " + cachedMessages.size());
        }
    }
    
    /**
     * Récupère un message avec gestion du cache
     */
    public String getMessage(String path, String def) {
        if (plugin.getConfig().getBoolean("optimization.enable-message-caching", true)) {
            return cachedMessages.getOrDefault(path, def);
        }
        return ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfig().getString("messages." + path, def));
    }
    
    /**
     * Met à jour l'hologramme avec gestion de la limitation de fréquence
     */
    public void updateHologram() {
        try {
            // Vérification du throttling
            if (plugin.getConfig().getBoolean("optimization.enable-hologram-update-throttling", true)) {
                long currentTime = System.currentTimeMillis();
                long delay = plugin.getConfig().getLong("optimization.hologram-update-delay", 1000);
                
                if (currentTime - lastHologramUpdate < delay) {
                    return; // Ne pas mettre à jour si le délai n'est pas écoulé
                }
            }
            
            String hologramName = plugin.getConfig().getString("hologram.name", "clickgame_hologram");
            
            // Récupérer l'hologramme existant
            this.hologram = DHAPI.getHologram(hologramName);
            if (this.hologram == null) {
                plugin.getLogger().warning("L'hologramme '" + hologramName + "' n'existe pas");
                return;
            }
            
            // Mettre à jour le contenu de l'hologramme
            updateHologramContent(this.hologram);
            lastHologramUpdate = System.currentTimeMillis();
            
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur lors de la mise à jour de l'hologramme: " + e.getMessage());
            if (plugin.getConfig().getBoolean("settings.debug.log-hologram-update", false)) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Met à jour le contenu de l'hologramme avec les scores actuels
     * @param hologram L'hologramme à mettre à jour
     */
    /**
     * Met à jour le contenu de l'hologramme avec les scores actuels
     * @param hologram L'hologramme à mettre à jour
     */
    private void updateHologramContent(Hologram hologram) {
        try {
            List<String> lines = new ArrayList<>();
            ConfigurationSection displaySection = plugin.getConfig().getConfigurationSection("hologram.display");
            if (displaySection == null) {
                plugin.getLogger().warning("La section de configuration 'hologram.display' est manquante !");
                return;
            }
            
            boolean debug = plugin.getConfig().getBoolean("settings.debug.log-hologram-update", false);
            int lastClicksToShow = displaySection.getInt("last-clicks", 10);
            
            // Mettre à jour automatiquement game-lines pour inclure le titre, le texte d'instruction, les clics et le bouton
            int gameLines = lastClicksToShow + 5; // +5 pour le titre, les 2 lignes de texte, 1 séparation et le bouton
            plugin.getConfig().set("settings.game-lines", gameLines);
            if (debug) {
                plugin.getLogger().info("[DEBUG] Mise à jour de game-lines à " + gameLines + " (last-clicks: " + lastClicksToShow + ")");
            }
            
            // Ajouter le titre personnalisé depuis la configuration
            String title = displaySection.getString("title", "&c1&7-&e2&7-&a3&7-&7Modo");
            lines.add(ChatColor.translateAlternateColorCodes('&', title));
            lines.add(""); // Ligne vide de séparation
            
            // Ajouter le texte d'instruction sur deux lignes
            lines.add(ChatColor.translateAlternateColorCodes('&', "&7Défies les modos en faisant augmenter le score"));
            lines.add(ChatColor.translateAlternateColorCodes('&', "&7Mais attention, car s'ils passent par là tout est fini !"));
            lines.add(""); // Ligne vide de séparation
            
            // Générer les faux joueurs si nécessaire
            List<String> mockNames = displaySection.getStringList("mock-names");
            if (mockNames.isEmpty() && displaySection.getBoolean("enable-mock-names", true)) {
                mockNames = generateMockNames(lastClicksToShow);
                displaySection.set("mock-names", mockNames);
                plugin.saveConfig();
                if (debug) {
                    plugin.getLogger().info("[DEBUG] Génération de " + lastClicksToShow + " faux joueurs");
                }
            }
            
            // Afficher l'historique des clics réels
            int realEntries = Math.min(clickHistory.size(), lastClicksToShow);
            int fakeEntriesNeeded = Math.max(0, lastClicksToShow - realEntries);
            
            // Afficher les derniers clics (du plus ancien au plus récent)
            int startIndex = Math.max(0, clickHistory.size() - lastClicksToShow);
            for (int i = startIndex; i < clickHistory.size(); i++) {
                Map.Entry<UUID, Map.Entry<Long, Integer>> entry = clickHistory.get(i);
                UUID playerId = entry.getKey();
                long clickTime = entry.getValue().getKey();
                int score = entry.getValue().getValue();
                boolean isReset = score == -1; // -1 indique une réinitialisation
                
                String playerName = "Joueur inconnu";
                String prefix = "";
                
                // Récupérer les informations du joueur
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    playerName = player.getName();
                    prefix = getPlayerPrefix(player);
                } else {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
                    if (offlinePlayer.hasPlayedBefore()) {
                        playerName = offlinePlayer.getName();
                    }
                }
                
                // Formater la date
                String timeStr = new SimpleDateFormat("HH:mm").format(new Date(clickTime));
                
                // Créer la ligne formatée selon le type d'action
                String format = isReset ? 
                    displaySection.getString("format-reset", "&d%time% &8| &b%prefix% %player% &7a réinitialisé !") :
                    displaySection.getString("format-click", "&d%time% &8| &6%player% &7monte à &b%score% &b!");
                
                String line = ChatColor.translateAlternateColorCodes('&', format
                    .replace("%time%", timeStr)
                    .replace("%player%", playerName)
                    .replace("%prefix%", prefix)
                    .replace("%score%", String.valueOf(score)));
                    
                lines.add(line);
                
                if (debug) {
                    plugin.getLogger().info("[DEBUG] Ajout de l'entrée: " + line);
                }
            }
            
            // Ajouter les faux joueurs si nécessaire
            if (fakeEntriesNeeded > 0) {
                for (int i = 0; i < fakeEntriesNeeded && i < mockNames.size(); i++) {
                    String timeStr = new SimpleDateFormat("HH:mm").format(new Date(System.currentTimeMillis() - (fakeEntriesNeeded - i) * 60000L));
                    String fakeLine = ChatColor.translateAlternateColorCodes('&', 
                        displaySection.getString("format-click", "&d%time% &8| &6%player% &7monte à &b%score%&b!")
                            .replace("%time%", timeStr)
                            .replace("%player%", mockNames.get(i))
                            .replace("%score%", String.valueOf((fakeEntriesNeeded - i) * 10)));
                    lines.add(fakeLine);
                }
            }
            
            // Ajouter le bouton cliquable directement sous la liste des clics
            lines.add(ChatColor.translateAlternateColorCodes('&', 
                displaySection.getString("click-button", "&a[+1]")));
            
            // Mettre à jour l'hologramme
            Location loc = hologram.getLocation();
            String hologramName = hologram.getName();
            
            // Supprimer l'ancien hologramme
            DHAPI.removeHologram(hologramName);
            
            // Créer un nouvel hologramme avec le contenu mis à jour
            this.hologram = DHAPI.createHologram(hologramName, loc, lines);
            
            if (debug) {
                plugin.getLogger().info("[DEBUG] Hologramme mis à jour avec " + lines.size() + " lignes");
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur lors de la mise à jour du contenu de l'hologramme: " + e.getMessage());
            if (plugin.getConfig().getBoolean("settings.debug.log-hologram-update", false)) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Sauvegarde les données du jeu dans le fichier de données
     */
    public void saveData() {
        try {
            // Créer le fichier s'il n'existe pas
            if (!dataFile.exists()) {
                if (!dataFile.getParentFile().exists()) {
                    dataFile.getParentFile().mkdirs();
                }
                dataFile.createNewFile();
            }
            
            // Sauvegarder les données
            dataConfig.set("best-score", bestScore);
            if (bestPlayerName != null) {
                dataConfig.set("best-player", bestPlayerName);
            }
            
            // Sauvegarder les clics des joueurs
            for (Map.Entry<UUID, Integer> entry : playerClicks.entrySet()) {
                dataConfig.set("player-clicks." + entry.getKey().toString(), entry.getValue());
            }
            
            // Sauvegarder les derniers temps de clic
            for (Map.Entry<UUID, Long> entry : lastClickTimes.entrySet()) {
                dataConfig.set("last-click-times." + entry.getKey().toString(), entry.getValue());
            }
            
            dataConfig.save(dataFile);
            
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de la sauvegarde des données: " + e.getMessage());
            if (plugin.getConfig().getBoolean("settings.debug.log-save-load", false)) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Récupère le préfixe d'un joueur avec LuckPerms
     * @param player Le joueur dont on veut le préfixe
     * @return Le préfixe du joueur ou une chaîne vide si non trouvé
     */
    private String getPlayerPrefix(Player player) {
        try {
            LuckPerms api = LuckPermsProvider.get();
            CachedMetaData metaData = api.getPlayerAdapter(Player.class).getMetaData(player);
            String prefix = metaData.getPrefix();
            return prefix != null ? ChatColor.translateAlternateColorCodes('&', prefix) : "";
        } catch (Exception e) {
            plugin.getLogger().warning("Impossible de récupérer le préfixe du joueur: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * Réinitialise le jeu et affiche un message avec l'historique des clics
     * @param player Le joueur qui réinitialise le jeu
     */
    /**
     * Réinitialise le jeu et affiche un message avec l'historique des clics
     * @param player Le joueur qui réinitialise le jeu
     */
    public void resetGame(Player player) {
        // Ajouter un message de réinitialisation comme un clic spécial à la fin de la liste
        clickHistory.add(new AbstractMap.SimpleEntry<>(player.getUniqueId(), 
            new AbstractMap.SimpleEntry<>(System.currentTimeMillis(), -1)));
            
        // Mettre à jour l'hologramme immédiatement
        updateHologram();
        
        // Limiter la taille de l'historique pour éviter les fuites de mémoire
        while (clickHistory.size() > 1000) {
            clickHistory.remove(clickHistory.size() - 1);
        }
        
        // Sauvegarder les données actuelles avant réinitialisation
        int oldBestScore = bestScore;
        String oldBestPlayer = bestPlayerName;
        
        // Réinitialiser les scores
        currentScore = 0;
        playerClicks.clear();
        lastClickTimes.clear();
        
        // Restaurer le meilleur score et le meilleur joueur
        bestScore = oldBestScore;
        bestPlayerName = oldBestPlayer;
        
        // Régénérer les noms factices si nécessaire
        if (plugin.getConfig().getBoolean("hologram.display.enable-mock-names", true)) {
            ConfigurationSection displaySection = plugin.getConfig().getConfigurationSection("hologram.display");
            if (displaySection != null) {
                int lastClicksToShow = displaySection.getInt("last-clicks", 10);
                List<String> mockNames = new ArrayList<>();
                String[] colors = {"&a", "&b", "&c", "&d", "&e", "&f", "&1", "&2", "&3", "&4", "&5", "&6", "&7", "&8", "&9"};
                
                for (int i = 0; i < lastClicksToShow; i++) {
                    String color = colors[i % colors.length];
                    mockNames.add(color + "Joueur" + (i + 1));
                }
                
                // Ne pas écraser les noms factices personnalisés s'ils existent déjà
                if (!displaySection.isSet("mock-names")) {
                    displaySection.set("mock-names", mockNames);
                    plugin.saveConfig();
                }
            }
        }
        
        // Mettre à jour l'hologramme
        updateHologram();
        
        // Sauvegarder les données
        saveData();
    }
    
    /**
     * Génère une liste de noms de faux joueurs avec des couleurs aléatoires
     * @param count Nombre de noms à générer
     * @return Liste des noms générés
     */
    private List<String> generateMockNames(int count) {
        List<String> names = new ArrayList<>();
        String[] colors = {"a", "b", "c", "d", "e", "f", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
        String[] prefixes = {"Joueur", "Player", "Joueur_"};
        
        Random random = new Random();
        Set<String> usedNames = new HashSet<>();
        
        for (int i = 1; i <= count && i <= 100; i++) { // Limite à 100 pour éviter les boucles infinies
            String color = colors[random.nextInt(colors.length)];
            String prefix = prefixes[random.nextInt(prefixes.length)];
            String suffix = random.nextBoolean() ? "" : String.valueOf((char)('A' + random.nextInt(26)));
            
            String name = "&" + color + prefix + i + suffix;
            
            // S'assurer que le nom est unique
            if (!usedNames.contains(name)) {
                usedNames.add(name);
                names.add(ChatColor.translateAlternateColorCodes('&', name));
            } else {
                i--; // Réessayer avec un autre nom
            }
        }
        
        return names;
    }
    
    public void onDisable() {
        // Sauvegarder les données à l'arrêt
        saveData();
        
        // Arrêt du pool de threads
        if (threadPool != null) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
