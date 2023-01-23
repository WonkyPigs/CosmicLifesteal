package dev.wonkypigs.cosmiclifesteal.Helpers;

import dev.wonkypigs.cosmiclifesteal.CosmicLifesteal;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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
                            target.getPlayer().kickPlayer(plugin.getConfig().getString("death-ban-message")
                                    .replace("{time}", String.valueOf(bantime))
                                    .replace("&", "§"));
                        });
                    }

                    // broadcast
                    if (plugin.getConfig().getBoolean("settings.death-ban-broadcast")) {
                        Bukkit.broadcastMessage(plugin.getConfig().getString("death-ban-broadcast-message")
                                .replace("{prefix}", plugin.prefix)
                                .replace("{player}", target.getName())
                                .replace("&", "§"));
                    }

                    sender.sendMessage(plugin.getConfig().getString("death-banned-player-message")
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

                    // get last death ban id
                    statement = plugin.getConnection()
                            .prepareStatement("SELECT ID FROM deathban_history WHERE UUID = ? ORDER BY ID DESC LIMIT 1");
                    statement.setString(1, target.getUniqueId().toString());
                    ResultSet resultSet = statement.executeQuery();
                    if (resultSet.next()) {
                        int id = resultSet.getInt("ID");
                        // update deathban history
                        statement = plugin.getConnection()
                                .prepareStatement("UPDATE deathban_history SET EXPIRY = ? WHERE UUID = ? AND ID = ?");
                        statement.setLong(1, System.currentTimeMillis());
                        statement.setString(2, target.getUniqueId().toString());
                        statement.setInt(3, id);
                        statement.executeUpdate();
                    }

                    sender.sendMessage(plugin.getConfig().getString("death-unbanned-player-message")
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

    public static Integer getDeathbanAmount(Player target) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PreparedStatement statement = plugin.getConnection()
                        .prepareStatement("SELECT COUNT(*) FROM deathban_history WHERE UUID = ?");
                statement.setString(1, target.getUniqueId().toString());
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    future.complete(resultSet.getInt(1));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
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
                        sender.sendMessage(plugin.getConfig().getString("is-death-banned-message")
                                .replace("{prefix}", plugin.prefix)
                                .replace("{player}", target.getName())
                                .replace("&", "§"));
                    } else {
                        sender.sendMessage(plugin.getConfig().getString("is-not-death-banned-message")
                                .replace("{prefix}", plugin.prefix)
                                .replace("{player}", target.getName())
                                .replace("&", "§"));
                    }
                } else {
                    sender.sendMessage(plugin.getConfig().getString("is-not-death-banned-message")
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
                    future.complete(expiry <= System.currentTimeMillis());
                } else {
                    future.complete(false);
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return future.join();
    }

    public static void deathbanHistory(Player player, OfflinePlayer target, int page) {
        if (!player.hasPermission("lifesteal.deathban.history")) {
            player.sendMessage(plugin.noPermMessage);
            return;
        }
        Inventory inv = plugin.getServer().createInventory(null, 36, target.getName() + "'s History | " + page);
        final int currpage = page-1;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PreparedStatement statement = null;

                // get ban history of player
                statement = plugin.getConnection()
                        .prepareStatement("SELECT * FROM deathban_history WHERE UUID=?");
                statement.setString(1, target.getUniqueId().toString());
                ResultSet result = statement.executeQuery();
                int total_items = 0;
                while (result.next()) {
                    total_items++;
                }
                if (total_items == 0) {
                    player.sendMessage(plugin.getConfig().getString("messages.no-deathban-history")
                            .replace("{prefix}", plugin.prefix)
                            .replace("{player}", target.getName())
                            .replace("&", "§"));
                    return;
                }
                for (int i = 27; i < 36; i++) {
                    ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName(" ");
                    item.setItemMeta(meta);
                    inv.setItem(i, item);
                }

                // setting stuff up
                int first_item = currpage * 27;
                int last_item = first_item + 26;
                if (last_item > total_items) {
                    last_item = total_items;
                }
                if (last_item < total_items) {
                    ItemStack item = new ItemStack(Material.GREEN_WOOL);
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName("&aNext Page".replace("&", "§"));
                    item.setItemMeta(meta);
                    inv.setItem(34, item);
                }
                if (first_item > 0) {
                    ItemStack item = new ItemStack(Material.RED_WOOL);
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName("&cPrevious Page".replace("&", "§"));
                    item.setItemMeta(meta);
                    inv.setItem(28, item);
                }

                int curr_item = -1;
                result = statement.executeQuery();
                while (result.next()) {
                    curr_item++;
                    if ((curr_item < first_item) || (curr_item > last_item)) {
                        continue;
                    }
                    long initiated = result.getLong("INITIATED");
                    long expiry = result.getLong("EXPIRY");
                    int id = result.getInt("ID");
                    ItemStack item = new ItemStack(Material.PAPER);
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName("&eDeathban #".replace("&", "§") + id);
                    List<String> lore = new ArrayList<>();
                    // get date of initiated and expiry
                    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                    String initiated_date = formatter.format(new Date(initiated));
                    lore.add("&7Initiated: &f".replace("&", "§") + initiated_date);
                    // is expired?
                    if (expiry <= System.currentTimeMillis()) {
                        lore.add("&7Expiry: &fExpired".replace("&", "§"));
                    } else {
                        String expiry_date = formatter.format(new Date(expiry));
                        lore.add("&7Expiry: &f".replace("&", "§") + expiry_date);
                    }
                    // get ban duration
                    String duration_string = millisToTime(expiry - initiated);
                    lore.add("&7Duration: &f".replace("&", "§") + duration_string);
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                    inv.addItem(item);
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.openInventory(inv);
                });
            }catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public static String millisToTime(long millis) {
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(millis));
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis));
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis));
        String duration_string = "";
        if (days > 0) {
            duration_string += days + "d ";
        }
        if (hours > 0) {
            duration_string += hours + "hr ";
        }
        if (minutes > 0) {
            duration_string += minutes + "min ";
        }
        if (seconds > 0) {
            duration_string += seconds + "sec";
        }
        return duration_string;
    }
}
