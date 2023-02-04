package dev.wonkypigs.cosmiclifesteal.Placeholders;

import dev.wonkypigs.cosmiclifesteal.CosmicLifesteal;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class PlaceholderManager extends PlaceholderExpansion {

    private final CosmicLifesteal plugin;

    public PlaceholderManager(CosmicLifesteal plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getAuthor() {
        return "WonkyPigs";
    }

    @Override
    public String getIdentifier() {
        return "lifesteal";
    }

    @Override
    public String getVersion() {
        return "1.3";
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String identifier) {
        if (offlinePlayer == null) {
            return "";
        } else if (!offlinePlayer.isOnline()) {
            return "";
        } else {
            Player player = offlinePlayer.getPlayer();
            switch (identifier.toLowerCase()) {
                case "hearts":
                    int phearts = (int) (player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getBaseValue() / 2);
                    return String.valueOf(phearts);
                case "deathbans":
                    return plugin.deathbanAmounts.get(player.getUniqueId()).toString();
            }
        }
        return null;
    }
}
