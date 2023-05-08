package space.hwahyang.invcaptive;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CommandsManager implements CommandExecutor {

    private void sendMessage(CommandSender sender, String message) {
        sender.sendMessage("[Inv-Captive] " + message.replace("&", "§"));
    }

    // https://www.digminecraft.com/lists/color_list_pc.php
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            ItemStack item = new ItemStack(Material.IRON_INGOT, 10);

            player.getInventory().addItem(item);

            sendMessage(player, Inv_Captive.getPlugin(Inv_Captive.class).getConfig().getString("name"));

            return true;
        } else if (sender instanceof ConsoleCommandSender) {
            sendMessage(sender, "&cOnly players can use this command.");
            return true;
        }
        return false;
    }
}
