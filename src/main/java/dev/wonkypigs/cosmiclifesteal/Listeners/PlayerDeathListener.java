package dev.wonkypigs.cosmiclifesteal.Listeners;

import dev.wonkypigs.cosmiclifesteal.CosmicLifesteal;
import dev.wonkypigs.cosmiclifesteal.Helpers.DeathEventHelper;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.List;

public class PlayerDeathListener implements Listener {

    private static final CosmicLifesteal plugin = CosmicLifesteal.getInstance();

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        List<String> disabledWorlds = plugin.getConfig().getStringList("disabled-worlds");
        if (disabledWorlds.contains(event.getEntity().getWorld().getName())) {
            return;
        }
        Player player = event.getEntity();
        Player killer = player.getKiller();
        if (killer instanceof Player) {
            double playerHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            double killerHealth = killer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            int healthLost = plugin.getConfig().getInt("hearts-lost-on-death")*2;
            int healthGained = plugin.getConfig().getInt("hearts-gained-on-kill")*2;
            if (plugin.getConfig().getBoolean("enable-head-drops")) {
                DeathEventHelper.dropHead(player, killer, event);
            }
            if (playerHealth - healthLost <= 0) {
                DeathEventHelper.deathBan(player);
            } else {
                DeathEventHelper.setHearts(player, (int) (playerHealth - healthLost)/2);
                // set health
                player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(playerHealth - healthLost);
                player.sendMessage(plugin.messageOnDeath);
            }
            if (killerHealth + healthGained >= plugin.getConfig().getInt("max-hearts")*2) {
                killer.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(plugin.getConfig().getInt("max-hearts")*2);
                killer.sendMessage(plugin.getConfig().getString("message-on-kill")
                        .replace("{prefix}", plugin.prefix)
                        .replace("{hearts}", String.valueOf((int) (plugin.getConfig().getInt("max-hearts")*2 - killerHealth)/2))
                        .replace("&", "§"));
                DeathEventHelper.setHearts(killer, plugin.getConfig().getInt("max-hearts"));
            } else {
                killer.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(killerHealth + healthGained);
                DeathEventHelper.setHearts(killer, (int) (killerHealth + healthGained)/2);
                killer.sendMessage(plugin.messageOnKill);
            }
        }
    }
}
