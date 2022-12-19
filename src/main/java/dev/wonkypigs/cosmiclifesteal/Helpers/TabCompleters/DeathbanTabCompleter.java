package dev.wonkypigs.cosmiclifesteal.Helpers.TabCompleters;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class DeathbanTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return List.of("ban", "unban", "check", "history");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("ban") || args[0].equalsIgnoreCase("unban") || args[0].equalsIgnoreCase("check") || args[0].equalsIgnoreCase("history"))) {
            OfflinePlayer[] offplayers = Bukkit.getOfflinePlayers();
            List<String> playerNames = new ArrayList<>();
            for (OfflinePlayer offplayer : offplayers) {
                playerNames.add(offplayer.getName());
            }
            return playerNames;
        } else {
            return null;
        }
    }
}
