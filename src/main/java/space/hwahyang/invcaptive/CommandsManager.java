package space.hwahyang.invcaptive;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandsManager {

    private final Inv_Captive inv_captive;

    public CommandsManager(Inv_Captive inv_captive) {
        this.inv_captive = inv_captive;
    }

    private void sendMessage(CommandSender sender, String message) {
        sender.sendMessage((inv_captive.getConfig().getString("messages.minecraft.prefix") + " " + message).replace("&", "§"));
    }

    // https://www.digminecraft.com/lists/color_list_pc.php
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            switch (label.toLowerCase()) {
                case "invcaptive", "captive" -> {
                    if (args.length == 0) return false;
                    switch (args[0].toLowerCase()) {
                        case "help" -> {
                            sendMessage(player, "Usage:");
                            sendMessage(player, String.format("/%s help", label));
                            sendMessage(player, String.format("/%s stat <PlayerName>", label));
                            sendMessage(player, String.format("/%s rank", label));
                            return true;
                            //break;
                        }
                        case "stat" -> { // optional Param: Nickname
                            String input = args.length == 1 ? player.getName() : args[1];
                            String result = inv_captive.getTeamManager().getTeamInfo(input, inv_captive.getConfig().getString("messages.minecraft.commands.teamInfo"), inv_captive::getInventory);
                            sendMessage(player, result == null ? String.format(inv_captive.getConfig().getString("messages.minecraft.commands.invalid"), input) : result);
                            return true;
                            //break;
                        }
                        case "rank" -> {
                            String result = inv_captive.getTeamManager().getTop5Teams(inv_captive.getConfig().getString("messages.minecraft.commands.ranking"), inv_captive::getInventory);
                            sendMessage(player, result);
                            return true;
                            //break;
                        }
                        default -> {
                            return false;
                            //break;
                        }
                    }
                    //break;
                }
                case "invcaptiveadmin", "captiveadmin" -> {
                    if (args.length == 0) return false;
                    switch (args[0].toLowerCase()) {
                        case "help" -> {
                            sendMessage(player, "Usage:");
                            sendMessage(player, String.format("/%s help", label));
                            sendMessage(player, String.format("/%s ender <PlayerName>", label));
                            return true;
                            //break;
                        }
                        case "ender" -> { // required Param: Nickname
                            if (args.length == 1) return false;

                            int team = inv_captive.getTeamManager().getPlayerTeam(args[1], inv_captive::getInventory);
                            if (team == -1) return false;

                            World separateWorld = Bukkit.getWorld("world_the_end_Group" + team);
                            if (separateWorld == null)
                                separateWorld = new WorldCreator("world_the_end_Group" + team).environment(World.Environment.THE_END).createWorld();

                            if (separateWorld == null) {
                                return false;
                            }

                            player.teleport(separateWorld.getSpawnLocation());
                            return true;
                            //break;
                        }
                        default -> {
                            return false;
                            //break;
                        }
                    }
                    //break;
                }
                default -> {
                    return false;
                    //break;
                }
            }
        } else {
            sendMessage(sender, "&cOnly players can use this command.");
            return true;
        }
    }
}
