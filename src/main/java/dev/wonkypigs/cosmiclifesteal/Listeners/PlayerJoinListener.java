package dev.wonkypigs.cosmiclifesteal.Listeners;

import dev.wonkypigs.cosmiclifesteal.CosmicLifesteal;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PlayerJoinListener implements Listener {

    private static final CosmicLifesteal plugin = CosmicLifesteal.getInstance();

    @EventHandler
    public void onPlayerAsyncJoin(AsyncPlayerPreLoginEvent e) {
        if (deathBanned(e)) {
            goAway(e);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        updateHearts(e.getPlayer());
    }

    private boolean deathBanned(AsyncPlayerPreLoginEvent e) {
        try {
            PreparedStatement statement = plugin.getConnection()
                    .prepareStatement("SELECT * FROM deathbans WHERE UUID = ?");
            statement.setString(1, e.getUniqueId().toString());
            ResultSet resultSet = statement.executeQuery();
            long expiry = 0;
            if (resultSet.next()) {
                expiry = resultSet.getLong("EXPIRY");
            }
            // expired?
            if (expiry <= System.currentTimeMillis()) {
                statement = plugin.getConnection()
                        .prepareStatement("DELETE FROM deathbans WHERE UUID = ?");
                statement.setString(1, e.getUniqueId().toString());
                statement.executeUpdate();
                return false;
            } else {
                return true;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    private void goAway(AsyncPlayerPreLoginEvent e) {
        try {
            // get ban time left
            PreparedStatement statement = plugin.getConnection()
                    .prepareStatement("SELECT EXPIRY FROM deathbans WHERE UUID = ?");
            statement.setString(1, e.getUniqueId().toString());
            ResultSet resultSet = statement.executeQuery();
            long expiry = 0;
            if (resultSet.next()) {
                expiry = resultSet.getLong("EXPIRY");
            }
            long timeLeft = expiry - System.currentTimeMillis();
            int minutesLeft = (int) (timeLeft / 1000 / 60);

            // kick
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, plugin.getConfig().getString("messages.death-ban-message")
                    .replace("{time}", String.valueOf(minutesLeft))
                    .replace("&", "§"));

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void updateHearts(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PreparedStatement statement = null;
                statement = plugin.getConnection()
                        .prepareStatement("SELECT * FROM hearts WHERE UUID = ?");
                statement.setString(1, player.getUniqueId().toString());
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    int hearts = resultSet.getInt("HEARTS");
                    player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(hearts*2);
                } else {
                    player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(plugin.getConfig().getInt("settings.starting-hearts")*2);
                    statement = plugin.getConnection()
                            .prepareStatement("INSERT INTO hearts (UUID, HEARTS) VALUES (?, ?)");
                    statement.setString(1, player.getUniqueId().toString());
                    statement.setInt(2, plugin.getConfig().getInt("settings.starting-hearts"));
                    statement.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
