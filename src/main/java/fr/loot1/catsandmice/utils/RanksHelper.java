package fr.loot1.catsandmice.utils;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.entity.Player;

public class RanksHelper {

    public RanksHelper() {}

    public String getPrefix(Player player) {
        LuckPerms api = LuckPermsProvider.get();
        CachedMetaData metaData = api.getPlayerAdapter(Player.class).getMetaData(player);
        return metaData.getPrefix();
    }

}
