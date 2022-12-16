package dev.wonkypigs.cosmiclifesteal.Helpers;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class LifestealTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "hearts");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("hearts")) {
            return List.of("add", "remove", "set", "get");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("hearts")) {
            OfflinePlayer[] offplayers = sender.getServer().getOfflinePlayers();
            List<String> playerNames = new ArrayList<>();
            for (OfflinePlayer offplayer : offplayers) {
                playerNames.add(offplayer.getName());
            }
            return playerNames;
        } else if (args.length == 4 && args[0].equalsIgnoreCase("hearts") && (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("set"))) {
            return List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
        }
        return null;
    }
}
