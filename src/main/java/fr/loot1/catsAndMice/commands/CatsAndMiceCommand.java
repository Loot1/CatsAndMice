package fr.loot1.catsAndMice.commands;

import fr.loot1.catsAndMice.CatsAndMice;
import fr.loot1.catsAndMice.utils.ConfigManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CatsAndMiceCommand implements CommandExecutor, TabExecutor {

    private final CatsAndMice main;
    private final ConfigManager configManager;

    public CatsAndMiceCommand(CatsAndMice regenWorld) {
        this.main = regenWorld;
        this.configManager = regenWorld.getConfigManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if(args.length > 0) {
            switch(args[0].toLowerCase()) {
                case "reload":
                    if(sender.hasPermission("catsandmice.reload")) {
                        configManager.reload();
                        sender.sendMessage(configManager.getColored("commands.reload.configuration-reload"));
                    } else {
                        sender.sendMessage(configManager.getColored("messages.errors.permission-denied"));
                    }
                    break;
                case "setredirectlocation":
                    if(sender.hasPermission("regenworld.setredirectlocation")) {
                        if (sender instanceof Player) {
                            Location playerLocation = ((Player) sender).getLocation();
                            sender.sendMessage(configManager.getColored("commands.set-redirect-location.location-set")
                                    .replace("%x%", String.valueOf(playerLocation.getBlockX()))
                                    .replace("%y%", String.valueOf(playerLocation.getBlockY()))
                                    .replace("%z%", String.valueOf(playerLocation.getBlockZ()))
                            );
                        } else {
                            sender.sendMessage(configManager.getColored("messages.errors.console-sender"));
                        }
                    } else {
                        sender.sendMessage(configManager.getColored("messages.errors.permission-denied"));
                    }
                    break;
                default:
                    if (sender.hasPermission("regenworld.help")) {
                        configManager.getColoredList("commands.help").forEach(sender::sendMessage);
                    } else {
                        sender.sendMessage(configManager.getColored("messages.errors.permission-denied"));
                    }
                    break;
            }
        } else {
            if (sender.hasPermission("regenworld.help")) {
                configManager.getColoredList("commands.help").forEach(sender::sendMessage);
            } else {
                sender.sendMessage(configManager.getColored("messages.errors.permission-denied"));
            }
        }
        return true;
    }

    private final List<String> SUBCOMMANDS = Arrays.asList("regen", "reload", "setredirectlocation", "worlds"); //create, rename, list
    private final List<String> WORLDS_SUBCOMMANDS = Arrays.asList("setschematic", "sety", "setborder", "delete");
    private final List<String> BLANK = Collections.emptyList();

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length <= 1) {
            return StringUtil.copyPartialMatches(args[0], SUBCOMMANDS, new ArrayList<>());
        } else if(args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "regen":
                    return StringUtil.copyPartialMatches(args[1], main.getServer().getWorlds().stream().map(World::getName).toList(), new ArrayList<>());
                case "worlds":
                    return StringUtil.copyPartialMatches(args[1], WORLDS_SUBCOMMANDS, new ArrayList<>());
                default:
                    break;
            }
        } else if(args[0].equalsIgnoreCase("worlds")) {
            switch(args.length) {
                case 3:
                    switch (args[1].toLowerCase()) {
                        case "setschematic":
                        case "sety":
                        case "setborder":
                        case "delete":
                            return StringUtil.copyPartialMatches(args[2], main.getServer().getWorlds().stream().map(World::getName).toList(), new ArrayList<>());
                        default:
                            break;
                    }
                    break;
                case 4:
                    switch (args[1].toLowerCase()) {
                        case "setschematic":
                            File schematicsFolder = new File(main.getDataFolder(), "schematics");
                            if (schematicsFolder.exists() && schematicsFolder.isDirectory()) {
                                String[] filesList = schematicsFolder.list();
                                if (filesList != null) {
                                    List<String> fileOptions = Arrays.stream(filesList)
                                            .map(s -> s.replace(".schem", ""))
                                            .toList();
                                    return StringUtil.copyPartialMatches(args[3], fileOptions, new ArrayList<>());
                                }
                            }
                        default:
                            break;
                    }
                    break;
            }
        }
        return BLANK;
    }

}
