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
@Permission(name = "catsandmice.create", desc = "Allows creating a game hologram", defaultValue = PermissionDefault.OP)
@Permission(name = "catsandmice.help", desc = "Displays the plugin help", defaultValue = PermissionDefault.OP)
@Permission(name = "catsandmice.reload", desc = "Allows reloading the plugin configuration", defaultValue = PermissionDefault.OP)

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
                            hologramManager.create(player.getLocation(), gameManager.getLastClicks(), gameManager.getLastBestClick());
                            sender.sendMessage(configManager.getComponent("messages.success.hologram-created"));
                        } else {
                            sender.sendMessage(configManager.getComponent("messages.errors.console-sender"));
                        }
                    } else {
                        sender.sendMessage(configManager.getComponent("messages.errors.permission-denied"));
                    }
                    break;
                case "reload":
                    if (sender.hasPermission("catsandmice.reload")) {
                        configManager.reload();
                        gameManager.refreshSettings();
                        hologramManager.refreshSettings();
                        hologramManager.update(gameManager.getLastClicks(), gameManager.getLastBestClick());
                        sender.sendMessage(configManager.getComponent("messages.success.configuration-reload"));
                    } else {
                        sender.sendMessage(configManager.getComponent("messages.errors.permission-denied"));
                    }
                    break;
                default:
                    if (sender.hasPermission("catsandmice.help")) {
                        configManager.getComponentList("messages.help").forEach(sender::sendMessage);
                    } else {
                        sender.sendMessage(configManager.getComponent("messages.errors.permission-denied"));
                    }
                    break;
            }
        } else {
            if (sender.hasPermission("catsandmice.help")) {
                configManager.getComponentList("messages.help").forEach(sender::sendMessage);
            } else {
                sender.sendMessage(configManager.getComponent("messages.errors.permission-denied"));
            }
        }
        return true;
    }

    private final List<String> SUBCOMMANDS = List.of("create", "help", "reload");
    private final List<String> BLANK = Collections.emptyList();

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length <= 1) {
            return StringUtil.copyPartialMatches(args[0], SUBCOMMANDS, new ArrayList<>());
        }
        return BLANK;
    }

}
