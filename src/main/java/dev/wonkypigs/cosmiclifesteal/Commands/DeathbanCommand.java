package dev.wonkypigs.cosmiclifesteal.Commands;

import dev.wonkypigs.cosmiclifesteal.CosmicLifesteal;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import static dev.wonkypigs.cosmiclifesteal.Helpers.DeathbanHelper.*;

public class DeathbanCommand implements CommandExecutor {

    private static final CosmicLifesteal plugin = CosmicLifesteal.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender.hasPermission("lifesteal.deathban")) {
            if (args.length == 0) {
                sender.sendMessage("&a&m----------------------------------------".replace("&", "§"));
                sender.sendMessage("&c/deathban ban &7<player>".replace("&", "§"));
                sender.sendMessage("&c/deathban unban &7<player>".replace("&", "§"));
                sender.sendMessage("&c/deathban check &7<player>".replace("&", "§"));
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
}
