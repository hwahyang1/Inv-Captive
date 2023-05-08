package space.hwahyang.invcaptive;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.commands.PluginSlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommandProvider;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.SubcommandData;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

// https://github.com/monun/inv-captive
public final class Inv_Captive extends JavaPlugin implements Listener, SlashCommandProvider {

    FileConfiguration config = getConfig();

    Logger logger = getLogger();

    File dataFolder;

    InventoryManager inventoryManager = new InventoryManager();

    private FileConfiguration inventoryData = null;
    private File inventoryDataFile = null;

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);

        this.saveDefaultConfig();

        getCommand("invCaptive").setExecutor(new CommandsManager());
        getCommand("invCaptiveAdmin").setExecutor(new CommandsManager());

        dataFolder = getDataFolder();

        logger.info("Inv-Captive Enabled.");
    }

    @Override
    public Set<PluginSlashCommand> getSlashCommands() {
        return new HashSet<>(Arrays.asList(
                new PluginSlashCommand(this, new CommandData("���", "���� ����մϴ�.")
                        .addOption(OptionType.STRING, "�÷��̾�1", "��� �� ù ��° �÷��̾ �����մϴ�.", true)
                        .addOption(OptionType.STRING, "�÷��̾�2", "��� �� �� ��° �÷��̾ �����մϴ�.")
                ),
                new PluginSlashCommand(this, new CommandData("����", "Ư�� �÷��̾�(��)�� �������� Ȯ���մϴ�.")
                        .addOption(OptionType.STRING, "�÷��̾�", "Ȯ�� �� �÷��̾ �����մϴ�.", true)
                ),
                new PluginSlashCommand(this, new CommandData("����", "�κ��丮 �ر� ������ Ȯ���մϴ�."))
        ));
    }

    @SlashCommand(path = "���")
    public void pingCommand(SlashCommandEvent event) {
        event.reply("Pong!").queue();
    }

    @SlashCommand(path = "����")
    public void bestPlugin(SlashCommandEvent event) {
        event.reply("DiscordSRV!").queue();
    }
    @SlashCommand(path = "����")
    public void bestFriend(SlashCommandEvent event) {
        event.reply("Dogs!").queue();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        //if (event.getPlayer().hasPermission("invCaptive.admin")) return;

        event.setJoinMessage(null);
        event.getPlayer().kickPlayer("��c��ϵ��� ���� �÷��̾�(��)�Դϴ�.\n���ڵ忡�� �� ��� ������ ���� ���� �� �ּ���.");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event.getPlayer().hasPermission("invCaptive.admin")) return;

    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent event) {
        saveInventory();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked().hasPermission("invCaptive.admin")) return;

        ItemStack itemStack = event.getCurrentItem();

        if (itemStack != null && itemStack.getType() == Material.BARRIER) {
            event.setCancelled(true);
            return;
        }

        if (event.getAction() == InventoryAction.HOTBAR_SWAP) {
            ItemStack itemStack2 = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
            if (itemStack2 != null && itemStack2.getType() == Material.BARRIER)
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (event.getPlayer().hasPermission("invCaptive.admin")) return;

        ItemStack itemStack = event.getItemDrop().getItemStack();
        if (itemStack.getType() == Material.BARRIER) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer().hasPermission("invCaptive.admin")) return;

    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getPlayer().hasPermission("invCaptive.admin")) return;

        ItemStack itemStack = event.getItem();
        if (itemStack != null && itemStack.getType() == Material.BARRIER) event.setCancelled(true);
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getPlayer().hasPermission("invCaptive.admin")) return;

        ItemStack itemStackMainHand = event.getPlayer().getInventory().getItemInMainHand();
        ItemStack itemStackOffHand = event.getPlayer().getInventory().getItemInOffHand();
        if (itemStackMainHand.getType() == Material.BARRIER) event.setCancelled(true);
        if (itemStackMainHand.getType() == Material.AIR && itemStackOffHand.getType() == Material.BARRIER) event.setCancelled(true);
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        ItemStack itemStack = event.getEntity().getItemStack();
        if (itemStack.getType() == Material.BARRIER) event.setCancelled(true);
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (event.getPlayer().hasPermission("invCaptive.admin")) return;

        ItemStack mainHandItemStack = event.getMainHandItem();
        ItemStack offHandItemStack = event.getOffHandItem();

        if (mainHandItemStack == null || offHandItemStack == null) return;
        if (offHandItemStack.getType() == Material.BARRIER || mainHandItemStack.getType() == Material.BARRIER)
            event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

    }

    @Override
    public void onDisable() {
        saveInventory();

        logger.info("Inv-Captive Disabled.");
    }

    public void saveInventory() {
        if (inventoryData == null) {
            inventoryDataFile = new File(dataFolder, "inventory.yml");
        }
        this.saveResource("inventory.yml", true);
    }

    public void reloadInventory() {
        if (inventoryDataFile == null) {
            inventoryDataFile = new File(dataFolder, "inventory.yml");
        }
        inventoryData = YamlConfiguration.loadConfiguration(inventoryDataFile);

        Reader defConfigStream = new InputStreamReader(this.getResource("inventory.yml"), StandardCharsets.UTF_8);
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            inventoryData.setDefaults(defConfig);
        }
    }

    public FileConfiguration getInventory() {
        if (inventoryData == null) {
            reloadInventory();
        }
        return inventoryData;
    }
}
