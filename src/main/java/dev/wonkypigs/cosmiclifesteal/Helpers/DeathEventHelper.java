package dev.wonkypigs.cosmiclifesteal.Helpers;

import dev.wonkypigs.cosmiclifesteal.CosmicLifesteal;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

public class DeathEventHelper {

    private static final CosmicLifesteal plugin = CosmicLifesteal.getInstance();

    public static void dropHead(Player player, Player killer, PlayerDeathEvent event) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        Timestamp timeOfDeath = new Timestamp(System.currentTimeMillis());
        String timeOfDeathString = timeOfDeath.toString().substring(8, 10) + "/" + timeOfDeath.toString().substring(5, 7) + "/" + timeOfDeath.toString().substring(0, 4) + " " + timeOfDeath.toString().substring(11, 19);

        headMeta.setDisplayName(plugin.getConfig().getString("dropped-head-name")
                .replace("{player}", player.getName())
                .replace("{killer}", killer.getName())
                .replace("{time_of_death}", timeOfDeathString)
                .replace("&", "§"));

        headMeta.setOwningPlayer(player);
        head.setItemMeta(headMeta);

        // set lore
        ArrayList<String> lore = new ArrayList<>();
        for (String line : plugin.getConfig().getStringList("dropped-head-lore")) {
            lore.add(line
                    .replace("{player}", player.getName())
                    .replace("{killer}", killer.getName())
                    .replace("{time_of_death}", timeOfDeathString)
                    .replace("&", "§"));
        }

        headMeta.setLore(lore);
        head.setItemMeta(headMeta);
        event.getDrops().add(head);
    }

    public static void deathBan(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PreparedStatement statement = null;
                int bantime = plugin.getConfig().getInt("death-ban-time");

                // get last deathban id of player
                statement = plugin.getConnection()
                        .prepareStatement("SELECT ID FROM deathban_history WHERE UUID = ? ORDER BY ID DESC LIMIT 1");
                statement.setString(1, player.getUniqueId().toString());
                ResultSet resultSet = statement.executeQuery();
                int id = 0;
                if (resultSet.next()) {
                    id = resultSet.getInt("ID");
                }

                // deathban player
                statement = plugin.getConnection()
                        .prepareStatement("INSERT INTO deathbans (UUID, EXPIRY) VALUES (?, ?)");
                statement.setString(1, player.getUniqueId().toString());
                statement.setLong(2, System.currentTimeMillis() + ((long) bantime * 60 * 1000));
                statement.executeUpdate();

                // add to deathban history
                statement = plugin.getConnection()
                        .prepareStatement("INSERT INTO deathban_history (UUID, INITIATED, EXPIRY, ID) VALUES (?, ?, ?, ?)");
                statement.setString(1, player.getUniqueId().toString());
                statement.setLong(2, System.currentTimeMillis());
                statement.setLong(3, System.currentTimeMillis() + ((long) bantime * 60 * 1000));
                statement.setInt(4, id + 1);
                statement.executeUpdate();

                // reset health
                HealthHelper.resetHearts(player);

                // kick
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.kickPlayer(plugin.getConfig().getString("death-ban-message")
                            .replace("{time}", String.valueOf(bantime))
                            .replace("&", "§"));
                });

                // broadcast
                if (plugin.getConfig().getBoolean("death-ban-broadcast")) {
                    Bukkit.broadcastMessage(plugin.getConfig().getString("death-ban-broadcast-message")
                            .replace("{prefix}", plugin.prefix)
                            .replace("{player}", player.getName())
                            .replace("&", "§"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public static void setHearts(Player target, int amount) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PreparedStatement statement = plugin.getConnection()
                        .prepareStatement("UPDATE hearts SET HEARTS = ? WHERE UUID = ?");
                statement.setInt(1, amount);
                statement.setString(2, target.getUniqueId().toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
