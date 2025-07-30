package fr.loot1.catsandmice.commands;

import fr.loot1.catsandmice.CatsAndMice;
import fr.loot1.catsandmice.managers.ConfigManager;
import fr.loot1.catsandmice.managers.GameManager;
import fr.loot1.catsandmice.managers.HologramManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.annotation.command.Commands;
import org.bukkit.plugin.java.annotation.permission.Permission;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Commands(@org.bukkit.plugin.java.annotation.command.Command(
        name = "catsandmice",
        desc = "Manage the CatsAndMice plugin",
        aliases = {"mice"}
))
@Permission(name = "catsandmice.create", desc = "Permet de générer un hologramme de jeu", defaultValue = PermissionDefault.OP)
@Permission(name = "catsandmice.help", desc = "Affiche l'aide du plugin", defaultValue = PermissionDefault.OP)
@Permission(name = "catsandmice.reload", desc = "Permet de recharger la configuration du plugin", defaultValue = PermissionDefault.OP)

public class CatsAndMiceCommand implements CommandExecutor, TabExecutor {

    private final ConfigManager configManager;
    private final GameManager gameManager;
    private final HologramManager hologramManager;

    public CatsAndMiceCommand(CatsAndMice catsAndMice) {
        this.configManager = catsAndMice.getConfigManager();
        this.gameManager = catsAndMice.getGameManager();
        this.hologramManager = catsAndMice.getHologramManager();
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if(args.length > 0) {
            switch(args[0].toLowerCase()) {
                case "create":
                    if (sender.hasPermission("catsandmice.create")) {
                        if(sender instanceof Player player) {
                            hologramManager.create(player.getLocation(), gameManager.getLastClicks());
                            sender.sendMessage(configManager.getColored("messages.success.hologram-created"));
                        } else {
                            sender.sendMessage(configManager.getColored("messages.errors.console-sender"));
                        }
                    } else {
                        sender.sendMessage(configManager.getColored("messages.errors.permission-denied"));
                    }
                    break;
                case "reload":
                    if (sender.hasPermission("catsandmice.reload")) {
                        configManager.reload();
                        hologramManager.update(gameManager.getLastClicks());
                        sender.sendMessage(configManager.getColored("messages.success.configuration-reload"));
                    } else {
                        sender.sendMessage(configManager.getColored("messages.errors.permission-denied"));
                    }
                    break;
                default:
                    if (sender.hasPermission("catsandmice.help")) {
                        configManager.getColoredList("messages.help").forEach(sender::sendMessage);
                    } else {
                        sender.sendMessage(configManager.getColored("messages.errors.permission-denied"));
                    }
                    break;
            }
        } else {
            if (sender.hasPermission("catsandmice.help")) {
                configManager.getColoredList("messages.help").forEach(sender::sendMessage);
            } else {
                sender.sendMessage(configManager.getColored("messages.errors.permission-denied"));
            }
        }
        return true;
    }

    private final List<String> SUBCOMMANDS = List.of("create", "reload");
    private final List<String> BLANK = Collections.emptyList();

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length <= 1) {
            return StringUtil.copyPartialMatches(args[0], SUBCOMMANDS, new ArrayList<>());
        }
        return BLANK;
    }

}
