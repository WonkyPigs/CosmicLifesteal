package dev.wonkypigs.cosmiclifesteal.Helpers;

import dev.wonkypigs.cosmiclifesteal.CosmicLifesteal;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class HealthHelper {

    private static final CosmicLifesteal plugin = CosmicLifesteal.getInstance();

    public static void addHearts(Player player, OfflinePlayer target, double am) {
        int amount = (int) Math.floor(am);
        if (!player.hasPermission("lifesteal.command.hearts.add")) {
            player.sendMessage(plugin.noPermMessage);
            return;
        } else if (amount < 1) {
            player.sendMessage(plugin.prefix + "&cYou must add at least 1 heart!".replace("&", "§"));
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PreparedStatement statement = null;

                // get current hearts
                statement = plugin.getConnection()
                        .prepareStatement("SELECT HEARTS FROM hearts WHERE UUID=?");
                statement.setString(1, target.getUniqueId().toString());
                ResultSet result = statement.executeQuery();
                if (!result.next()) {
                    player.sendMessage(plugin.invalidPlayerMessage);
                    return;
                }
                int hearts = result.getInt("HEARTS");

                if ((hearts + amount) > plugin.getConfig().getInt("settings.max-hearts")) {
                    player.sendMessage(plugin.prefix + "§cYou cannot set a player's hearts above " + plugin.getConfig().getInt("settings.max-hearts"));
                    return;
                }

                // add hearts
                statement = plugin.getConnection()
                        .prepareStatement("UPDATE hearts SET HEARTS=? WHERE UUID=?");
                statement.setInt(1,  hearts + amount);
                statement.setString(2, target.getUniqueId().toString());
                statement.executeUpdate();

                if (target.isOnline()) {
                    Player p2 = target.getPlayer();
                    p2.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(p2.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue() + (amount*2));
                }

                player.sendMessage(plugin.getConfig().getString("messages.hearts-added")
                        .replace("{prefix}", plugin.prefix)
                        .replace("{player}", target.getName())
                        .replace("{amount}", String.valueOf(amount))
                        .replace("&", "§"));

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public static void removeHearts(Player player, OfflinePlayer target, double am) {
        int amount = (int) Math.floor(am);
        if (!player.hasPermission("lifesteal.command.hearts.remove")) {
            player.sendMessage(plugin.noPermMessage);
            return;
        }
        if (amount < 1) {
            player.sendMessage(plugin.prefix + "&cYou must remove at least 1 heart!".replace("&", "§"));
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PreparedStatement statement = null;

                // get current hearts
                statement = plugin.getConnection()
                        .prepareStatement("SELECT HEARTS FROM hearts WHERE UUID=?");
                statement.setString(1, target.getUniqueId().toString());
                ResultSet result = statement.executeQuery();
                if (!result.next()) {
                    player.sendMessage(plugin.invalidPlayerMessage);
                    return;
                }
                int hearts = result.getInt("HEARTS");

                if (hearts-amount < 1) {
                    player.sendMessage(plugin.prefix + "§cThat player does not have enough hearts to remove!");
                    return;
                }

                // remove hearts
                statement = plugin.getConnection()
                        .prepareStatement("UPDATE hearts SET HEARTS=? WHERE UUID=?");
                statement.setInt(1,  hearts - amount);
                statement.setString(2, target.getUniqueId().toString());
                statement.executeUpdate();

                if (target.isOnline()) {
                    Player p2 = target.getPlayer();
                    p2.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(p2.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue() - (amount*2));
                }

                player.sendMessage(plugin.getConfig().getString("messages.hearts-removed")
                        .replace("{prefix}", plugin.prefix)
                        .replace("{player}", target.getName())
                        .replace("{amount}", String.valueOf(amount))
                        .replace("&", "§"));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public static void setHearts(Player player, OfflinePlayer target, double am) {
        int amount = (int) Math.floor(am);
        if (!player.hasPermission("lifesteal.command.hearts.set")) {
            player.sendMessage(plugin.noPermMessage);
            return;
        }
        else if (amount < 1) {
            player.sendMessage(plugin.prefix + "§cYou cannot set a player's hearts to less than 1!");
            return;
        }
        else if (amount > plugin.getConfig().getInt("settings.max-hearts")) {
            player.sendMessage(plugin.prefix + "§cYou cannot set a player's hearts above " + plugin.getConfig().getInt("settings.max-hearts"));
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PreparedStatement statement = null;
                // does player exist in database
                statement = plugin.getConnection()
                        .prepareStatement("SELECT * FROM hearts WHERE UUID=?");
                statement.setString(1, target.getUniqueId().toString());
                ResultSet result = statement.executeQuery();
                if (!result.next()) {
                    player.sendMessage(plugin.invalidPlayerMessage);
                    return;
                }

                // set hearts
                statement = plugin.getConnection()
                        .prepareStatement("UPDATE hearts SET HEARTS=? WHERE UUID=?");
                statement.setInt(1,  amount);
                statement.setString(2, target.getUniqueId().toString());
                statement.executeUpdate();

                if (target.isOnline()) {
                    Player p2 = target.getPlayer();
                    p2.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(amount*2);
                }

                player.sendMessage(plugin.getConfig().getString("messages.hearts-set")
                        .replace("{prefix}", plugin.prefix)
                        .replace("{player}", target.getName())
                        .replace("{amount}", String.valueOf(amount))
                        .replace("&", "§"));
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

    }

    public static void getHearts(Player player, OfflinePlayer target) {
        if (!player.hasPermission("lifesteal.command.hearts.get")) {
            player.sendMessage(plugin.noPermMessage);
            return;
        }
        if (target.isOnline()) {
            Player p2 = target.getPlayer();
            player.sendMessage(plugin.getConfig().getString("messages.hearts-get")
                    .replace("{prefix}", plugin.prefix)
                    .replace("{player}", target.getName())
                    .replace("{amount}", String.valueOf(p2.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue()/2))
                    .replace("&", "§"));
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    PreparedStatement statement = null;

                    // get current hearts
                    statement = plugin.getConnection()
                            .prepareStatement("SELECT HEARTS FROM hearts WHERE UUID=?");
                    statement.setString(1, target.getUniqueId().toString());
                    ResultSet result = statement.executeQuery();
                    if (!result.next()) {
                        player.sendMessage(plugin.invalidPlayerMessage);
                        return;
                    }
                    int hearts = result.getInt("HEARTS");

                    player.sendMessage(plugin.getConfig().getString("messages.hearts-get")
                            .replace("{prefix}", plugin.prefix)
                            .replace("{player}", target.getName())
                            .replace("{amount}", String.valueOf(hearts))
                            .replace("&", "§"));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public static void resetHearts(Player player) {
        try {
            PreparedStatement statement = plugin.getConnection()
                    .prepareStatement("UPDATE hearts SET HEARTS=? WHERE UUID=?");
            statement.setInt(1, plugin.getConfig().getInt("settings.hearts-after-deathban"));
            statement.setString(2, player.getUniqueId().toString());
            statement.executeUpdate();

            if (player.isOnline()) {
                player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(plugin.getConfig().getInt("settings.hearts-after-deathban")*2);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
