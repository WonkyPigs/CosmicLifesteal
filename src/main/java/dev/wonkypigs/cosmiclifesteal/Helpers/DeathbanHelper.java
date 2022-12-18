package dev.wonkypigs.cosmiclifesteal.Helpers;

import dev.wonkypigs.cosmiclifesteal.CosmicLifesteal;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import javax.xml.transform.Result;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public class DeathbanHelper {

    private static final CosmicLifesteal plugin = CosmicLifesteal.getInstance();

    public static void deathBan(CommandSender sender, OfflinePlayer target) {
        if (!sender.hasPermission("lifesteal.deathban.ban")) {
            sender.sendMessage(plugin.noPermMessage);
            return;
        }
        // Death ban a player if not already banned
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PreparedStatement statement = null;
                int bantime = plugin.getConfig().getInt("settings.death-ban-time");

                // is player already death banned?
                if (!checkDeathbanShort(target)) {
                    // get last death ban id
                    statement = plugin.getConnection()
                            .prepareStatement("SELECT ID FROM deathban_history WHERE UUID = ? ORDER BY ID DESC LIMIT 1");
                    statement.setString(1, target.getUniqueId().toString());
                    ResultSet resultSet = statement.executeQuery();
                    int id = 0;
                    if (resultSet.next()) {
                        id = resultSet.getInt("ID");
                    }

                    // deathban player
                    statement = plugin.getConnection()
                            .prepareStatement("INSERT INTO deathbans (UUID, EXPIRY) VALUES (?, ?)");
                    statement.setString(1, target.getUniqueId().toString());
                    statement.setLong(2, System.currentTimeMillis() + ((long) bantime * 60 * 1000));
                    statement.executeUpdate();

                    // add to deathban history
                    statement = plugin.getConnection()
                            .prepareStatement("INSERT INTO deathban_history (UUID, INITIATED, EXPIRY, ID) VALUES (?, ?, ?, ?)");
                    statement.setString(1, target.getUniqueId().toString());
                    statement.setLong(2, System.currentTimeMillis());
                    statement.setLong(3, System.currentTimeMillis() + ((long) bantime * 60 * 1000));
                    statement.setInt(4, id + 1);
                    statement.executeUpdate();

                    // reset health
                    statement = plugin.getConnection()
                            .prepareStatement("UPDATE hearts SET HEARTS = ? WHERE UUID = ?");
                    statement.setInt(1, plugin.getConfig().getInt("settings.hearts-after-deathban"));
                    statement.setString(2, target.getUniqueId().toString());
                    statement.executeUpdate();

                    // kick
                    if (target.isOnline()) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            target.getPlayer().kickPlayer(plugin.getConfig().getString("messages.death-ban-message")
                                    .replace("{time}", String.valueOf(bantime))
                                    .replace("&", "§"));
                        });
                    }

                    // broadcast
                    if (plugin.getConfig().getBoolean("settings.death-ban-broadcast")) {
                        Bukkit.broadcastMessage(plugin.getConfig().getString("settings.death-ban-broadcast-message")
                                .replace("{prefix}", plugin.prefix)
                                .replace("{player}", target.getName())
                                .replace("&", "§"));
                    }

                    sender.sendMessage(plugin.getConfig().getString("messages.death-banned-player")
                            .replace("{prefix}", plugin.prefix)
                            .replace("{player}", target.getName())
                            .replace("{time}", String.valueOf(bantime))
                            .replace("&", "§"));
                } else {
                    sender.sendMessage(plugin.prefix + "&cThat player is already death banned.".replace("&", "§"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public static void unDeathban(CommandSender sender, OfflinePlayer target) {
        if (!sender.hasPermission("lifesteal.deathban.unban")) {
            sender.sendMessage(plugin.noPermMessage);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PreparedStatement statement = null;

                // is player death banned?
                if (checkDeathbanShort(target)) {
                    // remove deathban
                    statement = plugin.getConnection()
                            .prepareStatement("DELETE FROM deathbans WHERE UUID = ?");
                    statement.setString(1, target.getUniqueId().toString());
                    statement.executeUpdate();

                    sender.sendMessage(plugin.getConfig().getString("messages.death-unbanned-player")
                            .replace("{prefix}", plugin.prefix)
                            .replace("{player}", target.getName())
                            .replace("&", "§"));
                } else {
                    sender.sendMessage(plugin.prefix + "&cThat player is not death banned.".replace("&", "§"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public static void checkDeathban(CommandSender sender, OfflinePlayer target) {
        if (!sender.hasPermission("lifesteal.deathban.check")) {
            sender.sendMessage(plugin.noPermMessage);
            return;
        }
        // check if a player is death banned
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PreparedStatement statement = plugin.getConnection()
                        .prepareStatement("SELECT * FROM deathbans WHERE UUID=?");
                statement.setString(1, target.getUniqueId().toString());
                ResultSet result = statement.executeQuery();

                if (result.next()) {
                    // if expiry already gone
                    int expiry = result.getInt("EXPIRY");
                    if (expiry < System.currentTimeMillis()) {
                        sender.sendMessage(plugin.getConfig().getString("messages.is-death-banned")
                                .replace("{prefix}", plugin.prefix)
                                .replace("{player}", target.getName())
                                .replace("&", "§"));
                    } else {
                        sender.sendMessage(plugin.getConfig().getString("messages.is-not-death-banned")
                                .replace("{prefix}", plugin.prefix)
                                .replace("{player}", target.getName())
                                .replace("&", "§"));
                    }
                } else {
                    sender.sendMessage(plugin.getConfig().getString("messages.is-not-death-banned")
                            .replace("{prefix}", plugin.prefix)
                            .replace("{player}", target.getName())
                            .replace("&", "§"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public static boolean checkDeathbanShort(OfflinePlayer target) {
        // check if a player is death banned but return boolean
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PreparedStatement statement = null;

                statement = plugin.getConnection()
                        .prepareStatement("SELECT * FROM deathbans WHERE UUID=?");
                statement.setString(1, target.getUniqueId().toString());
                ResultSet result = statement.executeQuery();

                if (result.next()) {
                    // if expiry already gone
                    int expiry = result.getInt("EXPIRY");
                    future.complete(expiry < System.currentTimeMillis());
                } else {
                    future.complete(false);
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return future.join();
    }
}
