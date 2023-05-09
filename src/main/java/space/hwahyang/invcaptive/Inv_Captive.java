package space.hwahyang.invcaptive;

import github.scarsz.discordsrv.api.commands.PluginSlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommandProvider;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

// https://github.com/monun/inv-captive
public final class Inv_Captive extends JavaPlugin implements CommandExecutor, Listener, SlashCommandProvider {

    private final FileConfiguration config = getConfig();

    private final Logger logger = getLogger();

    private final InventoryManager inventoryManager = new InventoryManager();
    private final TeamManager teamManager = new TeamManager();
    private final CommandsManager commandsManager = new CommandsManager(this);
    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }
    public TeamManager getTeamManager() {
        return teamManager;
    }

    private FileConfiguration inventoryData = null;
    private File inventoryDataFile = null;

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);

        this.saveDefaultConfig();

        reloadInventory();

        getCommand("invCaptive").setExecutor(this);
        getCommand("invCaptiveAdmin").setExecutor(this);

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
    public void addCommand(SlashCommandEvent event) {
        String p1 = event.getOption("플레이어1").getAsString().replaceAll("\\s+", "");
        String p2 = event.getOption("플레이어2") == null ? null : event.getOption("플레이어2").getAsString().replaceAll("\\s+", "");
        String response = "";
        if (p2 == null) {
            int data = teamManager.createTeam(this, new String[] {p1.toLowerCase()}, inventoryManager);
            response = data == -1 ? "플레이어 등록에 실패했습니다.\n등록한 플레이어가 이미 다른 팀에 등록되어 있을 수도 있습니다." : String.format("성공적으로 아래 플레이어를 팀으로 등록했습니다.\n%d팀: `%s`", data, p1);
        }
        else {
            int data = teamManager.createTeam(this, new String[] {p1.toLowerCase(), p2.toLowerCase()}, inventoryManager);
            response = data == -1 ? "플레이어 등록에 실패했습니다.\n등록한 플레이어가 이미 다른 팀에 등록되어 있을 수도 있습니다." : String.format("성공적으로 아래 플레이어를 팀으로 등록했습니다.\n%d팀: `%s`, `%s`", data, p1, p2);
        }
        event.reply(response).queue();
    }

    @SlashCommand(path = "정보")
    public void infoCommand(SlashCommandEvent event) {
        logger.info(event.getOption("플레이어").toString());
        event.reply("DiscordSRV!").queue();
    }

    @SlashCommand(path = "순위")
    public void rankCommand(SlashCommandEvent event) {
        event.reply("Dogs!").queue();
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (event.getPlayer().hasPermission("invCaptive.admin")) return;

        if (teamManager.getPlayerTeam(event.getPlayer().getName(), this::getInventory, this) == -1) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, getConfig().getString("deniedMessage").replace("&", "§"));
            return;
        }
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
    public static void onPortalTravel(PlayerPortalEvent event)
    {
        if (event.getPlayer().hasPermission("invCaptive.admin")) return;

        Player player = event.getPlayer();
        if (event.getCause() == PlayerPortalEvent.TeleportCause.END_PORTAL) {
            World separateWorld;
            if (Bukkit.getWorld("world_the_end_" + player.getName()) == null) {
                WorldCreator worldCreator = new WorldCreator("world_the_end_" + player.getName());
                worldCreator.environment(World.Environment.THE_END);
                separateWorld = Bukkit.createWorld(worldCreator);
                Bukkit.getServer().getWorlds().add(separateWorld);
            } else {
                separateWorld = Bukkit.getWorld("world_the_end_" + player.getName());
            }

            // End World의 스폰좌표는 포탈지점 -> 원래 엔드로 갈 때 스폰되어야 할 위치를 직접 찾아줘야 함!!!
            Location highestLocation = separateWorld.getHighestBlockAt(0, 0).getLocation();

            Block currentBlock = separateWorld.getBlockAt(highestLocation);
            while (currentBlock.getType() == Material.OBSIDIAN) {
                currentBlock = currentBlock.getRelative(BlockFace.DOWN);
            }

            Location spawnLocation = currentBlock.getRelative(BlockFace.UP).getLocation();
            separateWorld.setSpawnLocation(spawnLocation.getBlockX(), spawnLocation.getBlockY(), spawnLocation.getBlockZ());
        }
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
        event.setKeepInventory(true);

        if (event.getEntity().getPlayer() != null && event.getEntity().getPlayer().hasPermission("invCaptive.admin")) return;

        // 방벽을 제외한 나머지를 드랍시킴
        List<ItemStack> drops = event.getDrops();
        drops.clear();

        Inventory inventory = event.getEntity().getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack != null) {
                if (itemStack.getType() != Material.BARRIER) {
                    drops.add(itemStack);
                    inventory.setItem(i, null);
                }
            }
        }
    }

    @Override
    public void onDisable() {
        saveInventory();

        logger.info("Inv-Captive Disabled.");
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return commandsManager.onCommand(sender, command, label, args);
    }

    public void saveInventory() {
        try {
            inventoryData.save(inventoryDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reloadInventory() {
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
