package dev.wonkypigs.cosmiclifesteal.Commands;

import dev.wonkypigs.cosmiclifesteal.CosmicLifesteal;
import dev.wonkypigs.cosmiclifesteal.Helpers.HealthHelper;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class LifestealCommand implements CommandExecutor, TabCompleter {

    private static final CosmicLifesteal plugin = CosmicLifesteal.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender.hasPermission("lifesteal.command")) {
            if (args == null || args.length == 0) {
                sender.sendMessage("&a&m----------------------------------------".replace("&", "§"));
                sender.sendMessage("&c/lifesteal reload".replace("&", "§"));
                sender.sendMessage("&c/lifesteal hearts add &7<player> <amount>".replace("&", "§"));
                sender.sendMessage("&c/lifesteal hearts remove &7<player> <amount>".replace("&", "§"));
                sender.sendMessage("&c/lifesteal hearts set &7<player> <amount>".replace("&", "§"));
                sender.sendMessage("&c/lifesteal hearts get &7<player>".replace("&", "§"));
                sender.sendMessage("&7Try /deathban for more commands".replace("&", "§"));
                sender.sendMessage("&a&m----------------------------------------".replace("&", "§"));
            } else {
                if (args[0].equalsIgnoreCase("reload")) {
                    if (sender.hasPermission("lifesteal.command.reload")) {
                        plugin.updateConfig();
                        plugin.reloadConfig();
                        plugin.getConfigValues();
                        plugin.setupMessages();
                        plugin.mySqlSetup();
                        sender.sendMessage(plugin.prefix + "&aPlugin has been reloaded!".replace("&", "§"));
                    } else {
                        sender.sendMessage(plugin.noPermMessage);
                    }
                } else if (args[0].equalsIgnoreCase("hearts")) {
                    if (!sender.hasPermission("lifesteal.command.hearts")) {
                        sender.sendMessage(plugin.noPermMessage);
                        return true;
                    }
                    if (args.length > 2) {
                        OfflinePlayer target = null;
                        OfflinePlayer[] offplayers = plugin.getServer().getOfflinePlayers();
                        for (OfflinePlayer offplayer : offplayers) {
                            if (offplayer.getName().equalsIgnoreCase(args[2])) {
                                target = offplayer;
                                break;
                            }
                        }
                        if (target != null) {
                            if (args.length > 3 && !args[3].matches("[0-9]+")) {
                                sender.sendMessage(plugin.invalidArgumentsMessage);
                                return true;
                            }
                            if (args[1].equalsIgnoreCase("add") && args.length > 3) {
                                HealthHelper.addHearts(sender, target, Double.parseDouble(args[3]));
                            } else if (args[1].equalsIgnoreCase("remove") && args.length > 3) {
                                HealthHelper.removeHearts(sender, target, Double.parseDouble(args[3]));
                            } else if (args[1].equalsIgnoreCase("set") && args.length > 3) {
                                HealthHelper.setHearts(sender, target, Double.parseDouble(args[3]));
                            } else if (args[1].equalsIgnoreCase("get")) {
                                HealthHelper.getHearts(sender, target);
                            } else {
                                sender.sendMessage(plugin.invalidArgumentsMessage);
                            }
                        } else {
                            sender.sendMessage(plugin.invalidPlayerMessage);
                        }
                    } else {
                        sender.sendMessage(plugin.lifestealForHelp);
                    }
                } else {
                    sender.sendMessage(plugin.lifestealForHelp);
                }
            }
        } else {
            sender.sendMessage(plugin.noPermMessage);
        }
        return true;
    }

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
