package space.hwahyang.invcaptive;

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
import java.util.logging.Logger;

// https://github.com/monun/inv-captive
public final class Inv_Captive extends JavaPlugin implements Listener {

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

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent event) {

    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
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
        ItemStack itemStack = event.getItemDrop().getItemStack();
        if (itemStack.getType() == Material.BARRIER) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {

    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        ItemStack itemStack = event.getItem();
        assert itemStack != null;
        if (itemStack.getType() == Material.BARRIER) event.setCancelled(true);
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        ItemStack itemStack = event.getEntity().getItemStack();
        if (itemStack.getType() == Material.BARRIER) event.setCancelled(true);
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        ItemStack mainHandItemStack = event.getMainHandItem();
        ItemStack offHandItemStack = event.getOffHandItem();

        assert mainHandItemStack != null && offHandItemStack != null;
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
