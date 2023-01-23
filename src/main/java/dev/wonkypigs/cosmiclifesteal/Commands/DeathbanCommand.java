package dev.wonkypigs.cosmiclifesteal.Commands;

import dev.wonkypigs.cosmiclifesteal.CosmicLifesteal;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static dev.wonkypigs.cosmiclifesteal.Helpers.DeathbanHelper.*;

public class DeathbanCommand implements CommandExecutor, TabCompleter {

    private static final CosmicLifesteal plugin = CosmicLifesteal.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender.hasPermission("lifesteal.deathban")) {
            if (args.length == 0) {
                sender.sendMessage("&a&m----------------------------------------".replace("&", "§"));
                sender.sendMessage("&c/deathban ban &7<player>".replace("&", "§"));
                sender.sendMessage("&c/deathban unban &7<player>".replace("&", "§"));
                sender.sendMessage("&c/deathban check &7<player>".replace("&", "§"));
                sender.sendMessage("&c/deathban history &7<player>".replace("&", "§"));
                sender.sendMessage("&7Try /lifesteal for more commands".replace("&", "§"));
                sender.sendMessage("&a&m----------------------------------------".replace("&", "§"));
            } else if (args.length > 1) {
                OfflinePlayer target = null;
                OfflinePlayer[] offplayers = plugin.getServer().getOfflinePlayers();
                for (OfflinePlayer offplayer : offplayers) {
                    if (offplayer.getName().equalsIgnoreCase(args[1])) {
                        target = offplayer;
                        break;
                    }
                }
                if (target != null) {
                    if (args[0].equalsIgnoreCase("ban")) {
                        deathBan(sender, target);
                    } else if (args[0].equalsIgnoreCase("unban")) {
                        unDeathban(sender, target);
                    } else if (args[0].equalsIgnoreCase("check")) {
                        checkDeathban(sender, target);
                    } else if (args[0].equalsIgnoreCase("history")) {
                        if (sender instanceof Player) {
                            Player player = (Player) sender;
                            deathbanHistory(player, target, 1);
                        } else {
                            sender.sendMessage(plugin.bePlayerMessage);
                        }
                    } else {
                        sender.sendMessage(plugin.invalidArgumentsMessage);
                    }
                } else {
                    sender.sendMessage(plugin.invalidPlayerMessage);
                }
            } else {
                sender.sendMessage(plugin.deathbanForHelp);
            }
        } else {
            sender.sendMessage(plugin.noPermMessage);
        }
        return true;
    }

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
