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

    private final CatsAndMice main;
    private final ConfigManager configManager;
    private final DataFileManager dataFileManager;
    private final RanksHelper ranksHelper;

    private Hologram hologram;

    private int lastClicksToShow;
    private DateTimeFormatter timeFormatter;
    private DateTimeFormatter dayFormatter;
    private boolean recentOnBottom;

    public HologramManager(CatsAndMice catsAndMice, RanksHelper ranksHelper) {
        this.main = catsAndMice;
        this.configManager = catsAndMice.getConfigManager();
        this.dataFileManager = catsAndMice.getDataFileManager();
        this.ranksHelper = ranksHelper;
        loadSettings();
    }

    private void loadSettings() {
        lastClicksToShow = configManager.getInt("settings.last-clicks");
        String timeFormat = configManager.get("settings.time-format");
        timeFormatter = DateTimeFormatter
                .ofPattern(timeFormat != null && !timeFormat.isEmpty() ? timeFormat : "HH:mm")
                .withZone(ZoneId.systemDefault());
        dayFormatter = DateTimeFormatter
                .ofPattern("dd/MM/yyyy")
                .withZone(ZoneId.systemDefault());
        recentOnBottom = configManager.getBoolean("settings.recent-on-bottom");
    }

    public void refreshSettings() {
        loadSettings();
    }

    public void init(List<Click> lastClicks, Click bestClick) {
        String hologramName = configManager.get("settings.hologram-name");
        Hologram hologramExist = DHAPI.getHologram(hologramName);
        if (hologramExist != null) {
            this.hologram = hologramExist;
            update(lastClicks, bestClick);
        } else {
            Location location = dataFileManager.getLocation("hologram-location");
            if (location != null && location.getWorld() != null) {
                create(location, lastClicks, bestClick);
            }
        }
    }

    public void create(Location location, List<Click> lastClicks, Click bestClick) {
        String hologramName = configManager.get("settings.hologram-name");

        // Generate lines first to precisely calculate the height offset
        List<String> lines = generateLines(lastClicks, bestClick);
        double heightOffset = (lines.size() * 0.33) + 0.5;
        Location adjustedLocation = location.clone();
        adjustedLocation.setY(adjustedLocation.getY() + heightOffset);

        Hologram existingHologram = DHAPI.getHologram(hologramName);
        if (existingHologram != null) {
            DHAPI.moveHologram(existingHologram, adjustedLocation);
            this.hologram = existingHologram;
            update(lastClicks, bestClick);
        } else {
            this.hologram = DHAPI.createHologram(hologramName, adjustedLocation, true, lines);
        }

        dataFileManager.set("hologram-location", location);
    }

    public void update(List<Click> lastClicks, Click bestClick) {
        if (this.hologram == null) {
            this.hologram = DHAPI.getHologram(configManager.get("settings.hologram-name"));
            if (this.hologram == null) return;
        }

        List<String> newLines = generateLines(lastClicks, bestClick);
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

    private List<String> generateLines(List<Click> lastClicks, Click bestClick) {
        List<String> lines = new ArrayList<>();

        String bestScoreStr = bestClick != null ? String.valueOf(bestClick.getScore()) : "None";
        String bestPlayerName = bestClick != null ? getPlayerName(bestClick.getUUID()) : "None";
        String bestPrefix = bestClick != null ? getPlayerPrefix(bestClick.getUUID()) : "";
        String bestTimeStr = bestClick != null ? timeFormatter.format(Instant.ofEpochMilli(bestClick.getDate())) : "";
        String bestDayStr = bestClick != null ? dayFormatter.format(Instant.ofEpochMilli(bestClick.getDate())) : "";

        String[] bestPlaceholders = {"player", "score", "prefix", "time", "day"};
        String[] bestValues = {bestPlayerName, bestScoreStr, bestPrefix, bestTimeStr, bestDayStr};

        String title = configManager.getColored("messages.hologram.title");
        List<String> topDescription = configManager.getColoredListReplaced("messages.hologram.top-description", bestPlaceholders, bestValues);
        List<String> subDescription = configManager.getColoredListReplaced("messages.hologram.sub-description", bestPlaceholders, bestValues);

        // Title
        if (!title.isEmpty()) {
            lines.add(title);
        }

        // Top description (may contain best score placeholders)
        if (!topDescription.isEmpty()) {
            lines.addAll(topDescription);
        }

        // Calculate real and fake entries
        int realEntries = lastClicks.size();
        int fakeEntriesNeeded = Math.max(0, lastClicksToShow - realEntries);

        // Generate fake players if needed
        List<String> mockNames = dataFileManager.getStringList("mock-names");
        if (mockNames.isEmpty() && configManager.getBoolean("settings.enable-mock-names")) {
            mockNames = generateMockNames(lastClicksToShow);
            dataFileManager.set("mock-names", mockNames);
        }

        if (recentOnBottom) {
            if (fakeEntriesNeeded > 0) {
                for (int i = 0; i < fakeEntriesNeeded && i < mockNames.size(); i++) {
                    String timeStr = timeFormatter.format(Instant.ofEpochMilli(
                            System.currentTimeMillis() - (long) (fakeEntriesNeeded - i) * 60000L));
                    lines.add(configManager.getColoredReplaced("messages.hologram.click",
                            new String[]{"time", "player", "prefix", "score"},
                            new String[]{timeStr, mockNames.get(i), "", String.valueOf(fakeEntriesNeeded - i)}
                    ));
                }
            }
            // Real clicks from oldest to most recent
            List<Click> ascendingClicks = new ArrayList<>(lastClicks);
            Collections.reverse(ascendingClicks);
            for (Click clickToDisplay : ascendingClicks) {
                lines.add(buildClickLine(clickToDisplay));
            }
        } else {
            // Real clicks from most recent to oldest
            for (Click clickToDisplay : lastClicks) {
                lines.add(buildClickLine(clickToDisplay));
            }
            // Fake players after
            if (fakeEntriesNeeded > 0) {
                for (int i = 0; i < fakeEntriesNeeded && i < mockNames.size(); i++) {
                    String timeStr = timeFormatter.format(Instant.ofEpochMilli(
                            System.currentTimeMillis() - (long) (fakeEntriesNeeded - i) * 60000L));
                    lines.add(configManager.getColoredReplaced("messages.hologram.click",
                            new String[]{"time", "player", "prefix", "score"},
                            new String[]{timeStr, mockNames.get(i), "", String.valueOf(fakeEntriesNeeded - i)}
                    ));
                }
            }
        }

        // Bottom description
        if (!subDescription.isEmpty()) {
            lines.addAll(subDescription);
        }
        lines.add(configManager.getColored("messages.hologram.click-button"));
        return lines;
    }

    private String buildClickLine(Click click) {
        String timeStr = timeFormatter.format(Instant.ofEpochMilli(click.getDate()));
        boolean isReset = click.getScore() == 0;
        return configManager.getColoredReplaced(
                isReset ? "messages.hologram.reset" : "messages.hologram.click",
                new String[]{"time", "player", "prefix", "score"},
                new String[]{timeStr, getPlayerName(click.getUUID()), getPlayerPrefix(click.getUUID()), String.valueOf(click.getScore())});
    }

    private String getPlayerName(UUID uuid) {
        Player player = main.getServer().getPlayer(uuid);
        if (player != null) return player.getName();
        OfflinePlayer offlinePlayer = main.getServer().getOfflinePlayer(uuid);
        if (offlinePlayer.hasPlayedBefore() && offlinePlayer.getName() != null) {
            return offlinePlayer.getName();
        }
        return "Unknown player";
    }

    private String getPlayerPrefix(UUID uuid) {
        Player player = main.getServer().getPlayer(uuid);
        if (player != null) return ranksHelper.getPrefix(player);
        return "";
    }

    private List<String> generateMockNames(int count) {
        List<String> names = new ArrayList<>();
        String[] colors = {"a", "b", "c", "d", "e", "f", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
        String[] prefixes = {"Player", "User", "Player_"};

        Random random = new Random();
        Set<String> usedNames = new HashSet<>();
        int attempts = 0;
        int maxAttempts = count * 10;

        for (int i = 1; i <= count && attempts < maxAttempts; i++, attempts++) {
            String color = colors[random.nextInt(colors.length)];
            String prefix = prefixes[random.nextInt(prefixes.length)];
            String suffix = random.nextBoolean() ? "" : String.valueOf((char) ('A' + random.nextInt(26)));
            String name = "&" + color + prefix + i + suffix;
            String translated = ChatColor.translateAlternateColorCodes('&', name);
            if (usedNames.add(translated)) {
                names.add(translated);
            } else {
                i--;
            }
        }

        return names;
    }

}
