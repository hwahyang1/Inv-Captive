package space.hwahyang.invcaptive;

import github.scarsz.discordsrv.api.commands.PluginSlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommandProvider;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

// https://github.com/monun/inv-captive
public final class Inv_Captive extends JavaPlugin implements Listener, SlashCommandProvider {

    FileConfiguration config = getConfig();

    Logger logger = getLogger();

    File dataFolder;

    InventoryManager inventoryManager = new InventoryManager();
    CommandsManager commandsManager = new CommandsManager();

    private FileConfiguration inventoryData = null;
    private File inventoryDataFile = null;

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);

        this.saveDefaultConfig();

        getCommand("invCaptive").setExecutor(new CommandsManager());
        getCommand("invCaptiveAdmin").setExecutor(new CommandsManager());

        dataFolder = getDataFolder();

        /* ================================================ */
        logger.info(getInventory().getInt("groups.total") + "");
        logger.info(getInventory().getStringList("groups.group1.players").toString());
        logger.info(getInventory().getIntegerList("groups.group1.unlockItems").toString());
        logger.info(getInventory().getIntegerList("groups.group1.lastItems").toString());

        for (var c: inventoryManager.getMaterialArray()) {
            logger.info(c.name());
        }
        /* ================================================ */

        logger.info("Inv-Captive Enabled.");
    }

    @Override
    public Set<PluginSlashCommand> getSlashCommands() {
        return new HashSet<>(Arrays.asList(
                new PluginSlashCommand(this, new CommandData("등록", "팀을 등록합니다.")
                        .addOption(OptionType.STRING, "플레이어1", "등록 할 첫 번째 플레이어를 지정합니다.", true)
                        .addOption(OptionType.STRING, "플레이어2", "등록 할 두 번째 플레이어를 지정합니다.")
                ),
                new PluginSlashCommand(this, new CommandData("정보", "특정 플레이어(팀)의 진전도를 확인합니다.")
                        .addOption(OptionType.STRING, "플레이어", "확인 할 플레이어를 지정합니다.", true)
                ),
                new PluginSlashCommand(this, new CommandData("순위", "인벤토리 해금 순위를 확인합니다."))
        ));
    }

    @SlashCommand(path = "등록")
    public void pingCommand(SlashCommandEvent event) {
        logger.info(event.getOption("플레이어1").toString());
        logger.info(event.getOption("플레이어2").toString());
        event.reply("Pong!").queue();
    }

    @SlashCommand(path = "정보")
    public void bestPlugin(SlashCommandEvent event) {
        logger.info(event.getOption("플레이어").toString());
        event.reply("DiscordSRV!").queue();
    }

    @SlashCommand(path = "순위")
    public void bestFriend(SlashCommandEvent event) {
        event.reply("Dogs!").queue();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event.getPlayer().hasPermission("invCaptive.admin")) return;

        event.setJoinMessage(null);
        event.getPlayer().kickPlayer(getConfig().getString("deniedMessage").replace("&", "§"));
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
        /*if (inventoryData == null) {
            inventoryDataFile = new File(dataFolder, "inventory.yml");
        }
        this.saveResource("inventory.yml", true);*/
        try {
            inventoryData.save(inventoryDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reloadInventory() {
        /*if (inventoryDataFile == null) {
            inventoryDataFile = new File(dataFolder, "inventory.yml");
        }
        inventoryData = YamlConfiguration.loadConfiguration(inventoryDataFile);

        Reader defConfigStream = new InputStreamReader(this.getResource("inventory.yml"), StandardCharsets.UTF_8);
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            inventoryData.setDefaults(defConfig);
        }*/
        inventoryDataFile = new File(getDataFolder(), "inventory.yml");
        if (!inventoryDataFile.exists()) {
            inventoryDataFile.getParentFile().mkdirs();
            saveResource("inventory.yml", false);
        }

        inventoryData = new YamlConfiguration();
        try {
            inventoryData.load(inventoryDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public FileConfiguration getInventory() {
        if (inventoryData == null) {
            reloadInventory();
        }
        return inventoryData;
    }
}
