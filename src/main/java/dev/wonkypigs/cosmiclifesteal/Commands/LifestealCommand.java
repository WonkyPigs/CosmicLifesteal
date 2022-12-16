package dev.wonkypigs.cosmiclifesteal.Commands;

import dev.wonkypigs.cosmiclifesteal.CosmicLifesteal;
import dev.wonkypigs.cosmiclifesteal.Helpers.HealthHelper;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LifestealCommand implements CommandExecutor {

    private static final CosmicLifesteal plugin = CosmicLifesteal.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.hasPermission("lifesteal.command")) {
                if (args == null || args.length == 0) {
                    player.sendMessage("&a&m----------------------------------------".replace("&", "§"));
                    player.sendMessage("&c/lifesteal reload".replace("&", "§"));
                    player.sendMessage("&c/lifesteal hearts add &7<player> <amount>".replace("&", "§"));
                    player.sendMessage("&c/lifesteal hearts remove &7<player> <amount>".replace("&", "§"));
                    player.sendMessage("&c/lifesteal hearts set &7<player> <amount>".replace("&", "§"));
                    player.sendMessage("&c/lifesteal hearts get &7<player>".replace("&", "§"));
                    player.sendMessage("&a&m----------------------------------------".replace("&", "§"));
                } else {
                    if (args[0].equalsIgnoreCase("reload")) {
                        if (player.hasPermission("lifesteal.command.reload")) {
                            plugin.reloadConfig();
                            plugin.getConfigValues();
                            plugin.setupMessages();
                            plugin.mySqlSetup();
                            player.sendMessage(plugin.prefix + "&aPlugin has been reloaded!".replace("&", "§"));
                        } else {
                            player.sendMessage(plugin.noPermMessage);
                        }
                    } else if (args[0].equalsIgnoreCase("hearts")) {
                        if (!player.hasPermission("lifesteal.command.hearts")) {
                            player.sendMessage(plugin.noPermMessage);
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
                                if (args[1].equalsIgnoreCase("add") && args.length > 3) {
                                    if (!args[3].matches("[0-9]+")) {
                                        player.sendMessage(plugin.invalidArgumentsMessage);
                                        return true;
                                    }
                                    HealthHelper.addHearts(player, target, Double.parseDouble(args[3]));
                                } else if (args[1].equalsIgnoreCase("remove") && args.length > 3) {
                                    if (!args[3].matches("[0-9]+")) {
                                        player.sendMessage(plugin.invalidArgumentsMessage);
                                        return true;
                                    }
                                    HealthHelper.removeHearts(player, target, Double.parseDouble(args[3]));
                                } else if (args[1].equalsIgnoreCase("set") && args.length > 3) {
                                    if (!args[3].matches("[0-9]+")) {
                                        player.sendMessage(plugin.invalidArgumentsMessage);
                                        return true;
                                    }
                                    HealthHelper.setHearts(player, target, Double.parseDouble(args[3]));
                                } else if (args[1].equalsIgnoreCase("get")) {
                                    HealthHelper.getHearts(player, target);
                                } else {
                                    player.sendMessage(plugin.invalidArgumentsMessage);
                                }
                            } else {
                                player.sendMessage(plugin.invalidPlayerMessage);
                            }
                        } else {
                            player.sendMessage(plugin.lifestealForHelp);
                        }
                    } else {
                        player.sendMessage(plugin.lifestealForHelp);
                    }
                }
            } else {
                player.sendMessage(plugin.noPermMessage);
            }
        } else {
            sender.sendMessage(plugin.bePlayerMessage);
        }
        return true;
    }
}
