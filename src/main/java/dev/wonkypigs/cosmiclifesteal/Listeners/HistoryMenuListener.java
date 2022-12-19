package dev.wonkypigs.cosmiclifesteal.Listeners;

import dev.wonkypigs.cosmiclifesteal.CosmicLifesteal;
import dev.wonkypigs.cosmiclifesteal.Helpers.DeathbanHelper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class HistoryMenuListener implements Listener {

    private static final CosmicLifesteal plugin = CosmicLifesteal.getInstance();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().contains("'s History | ")) {
            e.setCancelled(true);
            if (e.getCurrentItem() != null) {
                String title = e.getView().getTitle();
                int currpage = Integer.parseInt(title.substring(title.indexOf("History | ") + 10));
                String pname  = title.substring(0, title.indexOf("'s"));
                Player player = (Player) e.getWhoClicked();
                OfflinePlayer target = null;

                OfflinePlayer[] players = Bukkit.getOfflinePlayers();
                for (OfflinePlayer p: players) {
                    if (pname.equals(p.getName())) {
                        target = p;
                    }
                }

                if (e.getCurrentItem().getType() == Material.GREEN_WOOL) {
                    currpage++;
                    assert target != null;
                    DeathbanHelper.deathbanHistory(player, target, currpage);
                } else if (e.getCurrentItem().getType() == Material.RED_WOOL) {
                    currpage--;
                    assert target != null;
                    DeathbanHelper.deathbanHistory(player, target, currpage);
                }
            }
        }
    }
}
