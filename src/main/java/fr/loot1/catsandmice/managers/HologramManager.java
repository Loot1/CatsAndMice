package fr.loot1.catsandmice.managers;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import fr.loot1.catsandmice.CatsAndMice;
import fr.loot1.catsandmice.Click;
import fr.loot1.catsandmice.utils.RanksHelper;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class HologramManager {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private final CatsAndMice main;
    private final ConfigManager configManager;
    private final DataFileManager dataFileManager;
    private final RanksHelper ranksHelper;

    private Hologram hologram;

    public HologramManager(CatsAndMice catsAndMice) {
        this.main = catsAndMice;
        this.configManager = catsAndMice.getConfigManager();
        this.dataFileManager = catsAndMice.getDataFileManager();
        this.ranksHelper = new RanksHelper();
    }

    public void init(List<Click> lastClicks) {
        String hologramName = configManager.get("settings.hologram-name");
        Hologram hologramExist = DHAPI.getHologram(hologramName);
        if (hologramExist != null) {
            this.hologram = hologramExist;
        } else {
            Location location = dataFileManager.getLocation("hologram-location");
            if (location != null && location.getWorld() != null) {
                create(location, lastClicks);
            }
        }
    }

    public void create(Location location, List<Click> lastClicks) {
        String hologramName = configManager.get("settings.hologram-name");

        int lastClicksToShow = configManager.getInt("settings.last-clicks");
        String title = configManager.getColored("messages.hologram.title");
        List<String> topDescription = configManager.getColoredList("messages.hologram.top-description");
        List<String> subDescription = configManager.getColoredList("messages.hologram.sub-description");
        int hologramLines = topDescription.size() + subDescription.size() + (title.isEmpty() ? 1 : 0) + lastClicksToShow + 1; // One line for the clickable button

        double heightOffset = (hologramLines * 0.33) + 0.5;
        Location adjustedLocation = location.clone();
        adjustedLocation.setY(adjustedLocation.getY() + heightOffset);

        Hologram existingHologram = DHAPI.getHologram(hologramName);
        if (existingHologram != null) {
            DHAPI.moveHologram(existingHologram, adjustedLocation);
            this.hologram = existingHologram;
        } else {
            List<String> lines = generateLines(lastClicks);
            this.hologram = DHAPI.createHologram(hologramName, adjustedLocation, true, lines);
        }

        dataFileManager.set("hologram-location", location);
    }

    public void update(List<Click> lastClicks) {
        if (this.hologram == null) {
            this.hologram = DHAPI.getHologram(configManager.get("settings.hologram-name"));
            if (this.hologram == null) return;
        }

        List<String> newLines = generateLines(lastClicks);
        int currentLineCount = this.hologram.getPage(0).getLines().size();

        if (currentLineCount == newLines.size()) {
            for (int i = 0; i < newLines.size(); i++) {
                DHAPI.setHologramLine(this.hologram, i, newLines.get(i));
            }
        } else {
            Location loc = this.hologram.getLocation();
            if (loc == null) return;
            String hologramName = this.hologram.getName();
            DHAPI.removeHologram(hologramName);
            this.hologram = DHAPI.createHologram(hologramName, loc, newLines);
        }
    }

    private List<String> generateLines(List<Click> lastClicks) {
        List<String> lines = new ArrayList<>();
        int lastClicksToShow = configManager.getInt("settings.last-clicks");

        String title = configManager.getColored("messages.hologram.title");
        List<String> topDescription = configManager.getColoredList("messages.hologram.top-description");
        List<String> subDescription = configManager.getColoredList("messages.hologram.sub-description");

        if (!title.isEmpty()) {
            lines.add(title);
        }
        if (!topDescription.isEmpty()) {
            lines.addAll(topDescription);
        }

        // Générer les faux joueurs si nécessaire
        List<String> mockNames = dataFileManager.getStringList("mock-names");
        if (mockNames.isEmpty() && configManager.getBoolean("settings.enable-mock-names")) {
            mockNames = generateMockNames(lastClicksToShow);
            dataFileManager.set("mock-names", mockNames);
        }

        // Afficher l'historique des clics réels
        int realEntries = Math.min(lastClicks.size(), lastClicksToShow);
        int fakeEntriesNeeded = Math.max(0, lastClicksToShow - realEntries);

        // Afficher les derniers clics (du plus ancien au plus récent)
        for (Click clickToDisplay : lastClicks) {
            UUID playerId = clickToDisplay.getUUID();
            long clickTime = clickToDisplay.getDate();
            int score = clickToDisplay.getScore();
            boolean isReset = score == 0;

            String playerName = "Joueur inconnu";
            String prefix = "";

            Player player = main.getServer().getPlayer(playerId);
            if (player != null) {
                playerName = player.getName();
                prefix = ranksHelper.getPrefix(player);
            } else {
                OfflinePlayer offlinePlayer = main.getServer().getOfflinePlayer(playerId);
                if (offlinePlayer.hasPlayedBefore()) {
                    playerName = offlinePlayer.getName();
                }
            }

            String timeStr = TIME_FORMATTER.format(Instant.ofEpochMilli(clickTime));

            lines.add(configManager.getColoredReplaced(
                    isReset ? "messages.hologram.reset" : "messages.hologram.click",
                    new String[]{"time", "player", "prefix", "score"},
                    new String[]{timeStr, playerName, prefix, String.valueOf(score)}));
        }

        if (fakeEntriesNeeded > 0) {
            for (int i = 0; i < fakeEntriesNeeded && i < mockNames.size(); i++) {
                String timeStr = TIME_FORMATTER.format(Instant.ofEpochMilli(System.currentTimeMillis() - (long)(fakeEntriesNeeded - i) * 60000L));
                lines.add(configManager.getColoredReplaced("messages.hologram.click",
                        new String[]{"time", "player", "score"},
                        new String[]{timeStr, mockNames.get(i), String.valueOf(fakeEntriesNeeded - i)}
                ));
            }
        }

        if (!subDescription.isEmpty()) {
            lines.addAll(subDescription);
        }
        lines.add(configManager.getColored("messages.hologram.click-button"));
        return lines;
    }

    private List<String> generateMockNames(int count) {
        List<String> names = new ArrayList<>();
        String[] colors = {"a", "b", "c", "d", "e", "f", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
        String[] prefixes = {"Joueur", "Player", "Joueur_"};

        Random random = new Random();
        Set<String> usedNames = new HashSet<>();

        for (int i = 1; i <= count && i <= 100; i++) { // Limite à 100 pour éviter les boucles infinies
            String color = colors[random.nextInt(colors.length)];
            String prefix = prefixes[random.nextInt(prefixes.length)];
            String suffix = random.nextBoolean() ? "" : String.valueOf((char) ('A' + random.nextInt(26)));

            String name = "&" + color + prefix + i + suffix;

            if (!usedNames.contains(name)) { // Unique names
                usedNames.add(name);
                names.add(ChatColor.translateAlternateColorCodes('&', name));
            } else {
                i--;
            }
        }

        return names;
    }

}
