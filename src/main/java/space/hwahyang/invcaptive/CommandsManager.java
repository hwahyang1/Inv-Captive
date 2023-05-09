package space.hwahyang.invcaptive;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandsManager {

    private final Inv_Captive inv_captive;

    public CommandsManager(Inv_Captive inv_captive) {
        this.inv_captive = inv_captive;
    }

    private void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(("[Inv-Captive] " + message).replace("&", "§"));
    }

    // https://www.digminecraft.com/lists/color_list_pc.php
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            switch (label.toLowerCase()) {
                case "invcaptive", "captive" -> {
                    if (args.length == 0) return false;
                    switch (args[0].toLowerCase()) {
                        case "help" -> {
                            sendMessage(player, String.format("Usage:\n - /%s help\n - /%s stat <PlayerName>\n - /%s rank", label, label, label));
                            return true;
                            //break;
                        }
                        case "stat" -> { // optional Param: Nickname
                            List<String> asdf = inv_captive.getInventory().getStringList("groups.group3.unlockItems");
                            var wqer = inv_captive.getInventoryManager().convertStringArrayToMaterialArray(asdf);
                            for (Material material: wqer) {
                                sendMessage(player, material.name());
                            }
                            sendMessage(player, "stat");
                            return true;
                            //break;
                        }
                        case "rank" -> {
                            sendMessage(player, "rank");
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
                            sendMessage(player, String.format("Usage:\n - /%s help\n - /%s ender <Team/PlayerName>", label, label));
                            return true;
                            //break;
                        }
                        case "ender" -> { // required Param: Team/Nickname
                            sendMessage(player, "ender");
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
