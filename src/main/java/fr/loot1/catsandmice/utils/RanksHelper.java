package fr.loot1.catsandmice.utils;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class RanksHelper {

    public RanksHelper() {}

    public String getPrefix(Player player) {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            return "";
        }
        try {
            LuckPerms api = LuckPermsProvider.get();
            CachedMetaData metaData = api.getPlayerAdapter(Player.class).getMetaData(player);
            String prefix = metaData.getPrefix();
            return prefix != null ? prefix : "";
        } catch (IllegalStateException e) {
            return "";
        }
    }

}