package space.hwahyang.invcaptive;

import github.scarsz.discordsrv.api.commands.PluginSlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommandProvider;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author HwaHyang
 * @original monun/inv-captive
 */
public final class Inv_Captive extends JavaPlugin implements CommandExecutor, Listener, SlashCommandProvider {

    private final FileConfiguration config = getConfig();

    private final Logger logger = getLogger();

    private final InventoryManager inventoryManager = new InventoryManager(this);
    private final TeamManager teamManager = new TeamManager(this);
    private final CommandsManager commandsManager = new CommandsManager(this);
    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }
    public TeamManager getTeamManager() {
        return teamManager;
    }

    private FileConfiguration inventoryData = null;
    private File inventoryDataFile = null;

    @SuppressWarnings("unchecked")
    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);

        this.saveDefaultConfig();

        this.reloadInventory();

        getCommand("invCaptive").setExecutor(this);
        getCommand("invCaptiveAdmin").setExecutor(this);

        for (Player player: Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("invCaptive.admin")) continue;

            int team = teamManager.getPlayerTeam(player.getName(), this::getInventory);
            if (team == -1) continue;

            player.setDisplayName(String.format(getConfig().getString("messages.minecraft.playerName").replace("&", "§"), team, player.getName()));
            player.setPlayerListName(String.format(getConfig().getString("messages.minecraft.playerName").replace("&", "§"), team, player.getName()));

            // 엔드가 로드되어 있지 않은 경우, 로드
            World separateWorld = Bukkit.getWorld("world_the_end_Group" + team);
            if (separateWorld == null)
                separateWorld = new WorldCreator("world_the_end_Group" + team).environment(World.Environment.THE_END).createWorld();

            String rootKey = String.format("groups.group%d.", team);
            List<?> lastInventoryData = getInventory().getList(rootKey + "lastItems");
            inventoryManager.apply(player, (List<ItemStack>) lastInventoryData);
        }

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
            response = data == -1 ? getConfig().getString("messages.discord.teamRegisterFailed") : String.format(getConfig().getString("messages.discord.teamRegistered.single").replace("&", "§"), data, p1);
        }
        else {
            int data = teamManager.createTeam(this, new String[] {p1.toLowerCase(), p2.toLowerCase()}, inventoryManager);
            response = data == -1 ? getConfig().getString("messages.discord.teamRegisterFailed") : String.format(getConfig().getString("messages.discord.teamRegistered.duo").replace("&", "§"), data, p1, p2);
        }
        event.reply(response).queue();
    }

    @SlashCommand(path = "정보")
    public void infoCommand(SlashCommandEvent event) {
        String input = event.getOption("플레이어").getAsString().toLowerCase();
        String result = teamManager.getTeamInfo(input, getConfig().getString("messages.discord.commands.teamInfo"), this::getInventory);

        event.reply(result == null ? String.format(getConfig().getString("messages.discord.commands.invalid"), input) : result).queue();
    }

    @SlashCommand(path = "순위")
    public void rankCommand(SlashCommandEvent event) {
        String result = teamManager.getTop5Teams(getConfig().getString("messages.discord.commands.ranking"), this::getInventory);

        event.reply(result).queue();
    }

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        event.setMotd(getConfig().getString("messages.minecraft.motd").replace("&", "§"));

        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        event.setMaxPlayers(Integer.parseInt(today.format(formatter)));
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("invCaptive.admin")) return;

        int team = teamManager.getPlayerTeam(player.getName(), this::getInventory);
        if (team == -1) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, (getConfig().getString("messages.minecraft.prefix") + "\n" + getConfig().getString("messages.minecraft.messages.unregisteredTeam")).replace("&", "§"));
            return;
        }

        // 엔드가 로드되어 있지 않은 경우, 로드
        World separateWorld = Bukkit.getWorld("world_the_end_Group" + team);
        if (separateWorld == null)
            separateWorld = new WorldCreator("world_the_end_Group" + team).environment(World.Environment.THE_END).createWorld();
    }

    @SuppressWarnings("unchecked")
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("invCaptive.admin")) return;

        // 검증 및 엔드 로드는 onPlayerLogin에서 모두 처리했음 -> 나머지 수행

        int team = teamManager.getPlayerTeam(player.getName(), this::getInventory);

        player.setDisplayName(String.format(getConfig().getString("messages.minecraft.playerName").replace("&", "§"), team, player.getName()));
        player.setPlayerListName(String.format(getConfig().getString("messages.minecraft.playerName").replace("&", "§"), team, player.getName()));

        event.setJoinMessage(String.format(("&e" + getConfig().getString("messages.minecraft.playerName") + "님이 서버에 접속했습니다.").replace("&", "§"), team, player.getName()));

        if (player.getFirstPlayed() == 0) {
            List<String> teamPlayers = teamManager.getTeamPlayers(team, this::getInventory);
            for (String current: teamPlayers) {
                // 찾는 대상이 현재 플레이어 이름과 같다면 무시
                if (current.equals(player.getName().toLowerCase())) continue;

                for (Player onlinePlayer: Bukkit.getOnlinePlayers()) {
                    // 찾는 대상과 온라인 대상이 같다면 이동
                    if (current.equals(onlinePlayer.getName().toLowerCase())) {
                        player.teleport(onlinePlayer);
                        break;
                    }
                }
            }
        }

        String rootKey = String.format("groups.group%d.", team);
        List<?> lastInventoryData = getInventory().getList(rootKey + "lastItems");
        inventoryManager.apply(player, (List<ItemStack>) lastInventoryData);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("invCaptive.admin")) return;

        int team = teamManager.getPlayerTeam(player.getName(), this::getInventory);

        event.setQuitMessage(String.format(("&e" + getConfig().getString("messages.minecraft.playerName") + "님이 서버를 나갔습니다.").replace("&", "§"), team, player.getName()));

        String rootKey = String.format("groups.group%d.", team);
        List<ItemStack> lastInventoryData = inventoryManager.export(player);
        getInventory().set(rootKey + "lastItems", lastInventoryData);
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent event) {
        List<Integer> processedTeams = new ArrayList<Integer>();
        for (Player player: Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("invCaptive.admin")) continue;

            int team = teamManager.getPlayerTeam(player.getName(), this::getInventory);
            if (processedTeams.contains(team)) continue;

            String rootKey = String.format("groups.group%d.", team);
            List<ItemStack> lastInventoryData = inventoryManager.export(player);
            getInventory().set(rootKey + "lastItems", lastInventoryData);
            processedTeams.add(team);
        }

        saveInventory();
        reloadInventory();
    }

    @EventHandler
    public void onPortalTravel(PlayerPortalEvent event)
    {
        Player player = event.getPlayer();

        if (player.hasPermission("invCaptive.admin")) return;

        if (event.getCause() == PlayerPortalEvent.TeleportCause.END_PORTAL) {
            int team = teamManager.getPlayerTeam(player.getName(), this::getInventory);
            World separateWorld = Bukkit.getWorld("world_the_end_Group" + team);
            if (separateWorld == null)
                separateWorld = new WorldCreator("world_the_end_Group" + team).environment(World.Environment.THE_END).createWorld();

            if (separateWorld == null) {
                event.setCancelled(true);
                return;
            }

            event.setTo(separateWorld.getSpawnLocation());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (player.hasPermission("invCaptive.admin")) return;

        ItemStack itemStack = event.getCurrentItem();

        if (itemStack != null && itemStack.getType() == Material.BARRIER) {
            event.setCancelled(true);
            return;
        }

        if (event.getAction() == InventoryAction.HOTBAR_SWAP) {
            ItemStack itemStack2 = player.getInventory().getItem(event.getHotbarButton());
            if (itemStack2 != null && itemStack2.getType() == Material.BARRIER) {
                event.setCancelled(true);
                return;
            }
        }

        Bukkit.getScheduler().runTask(this, () -> syncPlayerTeam(player));
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("invCaptive.admin")) return;

        ItemStack itemStack = event.getItemDrop().getItemStack();
        if (itemStack.getType() == Material.BARRIER) event.setCancelled(true);

        Bukkit.getScheduler().runTask(this, () -> syncPlayerTeam(player));
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (player.hasPermission("invCaptive.admin")) return;

        Bukkit.getScheduler().runTask(this, () -> syncPlayerTeam(player));
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (event.getPlayer().hasPermission("invCaptive.admin")) return;

        Bukkit.getScheduler().runTask(this, () -> syncPlayerTeam(player));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (event.getPlayer().hasPermission("invCaptive.admin")) return;

        Inventory inventory = player.getInventory();
        Material block = event.getBlock().getType();
        int team = teamManager.getPlayerTeam(player.getName(), this::getInventory);
        List<Material> unlockItems = teamManager.getTeamUnlockItems(team, this::getInventory);
        Material material;
        for (int i = 0; i < unlockItems.size(); i++) {
            // 이미 열린 칸은 지나감
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() != Material.BARRIER) continue;

            material = unlockItems.get(i);

            // 해금 조건 달성
            if (block.name().equals(material.name())) {
                getServer().broadcastMessage(String.format((/*getConfig().getString("messages.minecraft.prefix") + " " + */getConfig().getString("messages.minecraft.messages.inventoryUnlocked")).replace("&", "§"), player.getName(), team, block.name()));

                Location location = player.getLocation();
                FireworkEffect effect = FireworkEffect.builder()
                        .withColor(Color.YELLOW)
                        .withFade(Color.GRAY)
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .trail(true)
                        .build();
                Firework firework = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);
                FireworkMeta meta = firework.getFireworkMeta();
                meta.addEffect(effect);
                firework.setFireworkMeta(meta);
                location.getWorld().playSound(location, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1, 1);

                inventoryManager.openInventorySlot(player, i);
                Bukkit.getScheduler().runTask(this, () -> syncPlayerTeam(player));
                break;
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getPlayer().hasPermission("invCaptive.admin")) return;

        ItemStack itemStack = event.getItem();
        if (itemStack != null) {
            if (itemStack.getType() == Material.BARRIER) {
                event.setCancelled(true);
                return;
            }

            Material itemType = event.getItem().getType();
            if (itemType == Material.LEATHER_HELMET ||
                itemType == Material.CHAINMAIL_HELMET ||
                itemType == Material.IRON_HELMET ||
                itemType == Material.GOLDEN_HELMET ||
                itemType == Material.DIAMOND_HELMET ||
                itemType == Material.NETHERITE_HELMET) {
                if (player.getInventory().getItem(EquipmentSlot.HEAD).getType() == Material.BARRIER) {
                    event.setCancelled(true);
                    return;
                }
            }
            if (itemType == Material.LEATHER_CHESTPLATE ||
                itemType == Material.CHAINMAIL_CHESTPLATE ||
                itemType == Material.IRON_CHESTPLATE ||
                itemType == Material.GOLDEN_CHESTPLATE ||
                itemType == Material.DIAMOND_CHESTPLATE ||
                itemType == Material.NETHERITE_CHESTPLATE) {
                if (player.getInventory().getItem(EquipmentSlot.CHEST).getType() == Material.BARRIER) {
                    event.setCancelled(true);
                    return;
                }
            }
            if (itemType == Material.LEATHER_LEGGINGS ||
                itemType == Material.CHAINMAIL_LEGGINGS ||
                itemType == Material.IRON_LEGGINGS ||
                itemType == Material.GOLDEN_LEGGINGS ||
                itemType == Material.DIAMOND_LEGGINGS ||
                itemType == Material.NETHERITE_LEGGINGS) {
                if (player.getInventory().getItem(EquipmentSlot.LEGS).getType() == Material.BARRIER) {
                    event.setCancelled(true);
                    return;
                }
            }
            if (itemType == Material.LEATHER_BOOTS ||
                itemType == Material.CHAINMAIL_BOOTS ||
                itemType == Material.IRON_BOOTS ||
                itemType == Material.GOLDEN_BOOTS ||
                itemType == Material.DIAMOND_BOOTS ||
                itemType == Material.NETHERITE_BOOTS) {
                if (player.getInventory().getItem(EquipmentSlot.FEET).getType() == Material.BARRIER) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        Bukkit.getScheduler().runTask(this, () -> syncPlayerTeam(player));
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("invCaptive.admin")) return;

        Entity target = event.getRightClicked();

        if (target.getType() == EntityType.ITEM_FRAME || target.getType() == EntityType.GLOW_ITEM_FRAME) {
            ItemStack itemStackMainHand = player.getInventory().getItemInMainHand();
            ItemStack itemStackOffHand = player.getInventory().getItemInOffHand();
            if (itemStackMainHand.getType() == Material.BARRIER) {
                event.setCancelled(true);
                return;
            }
            if (itemStackMainHand.getType() == Material.AIR && itemStackOffHand.getType() == Material.BARRIER) {
                event.setCancelled(true);
                return;
            }
        }

        Bukkit.getScheduler().runTask(this, () -> syncPlayerTeam(player));
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack itemStack = event.getEntity().getItemStack();
        if (itemStack.getType() == Material.BARRIER) {
            event.setCancelled(true);
            return;
        }

        //Bukkit.getScheduler().runTask(this, () -> syncPlayerTeam(player));
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("invCaptive.admin")) return;

        ItemStack mainHandItemStack = event.getMainHandItem();
        ItemStack offHandItemStack = event.getOffHandItem();

        if (mainHandItemStack == null || offHandItemStack == null) return;
        if (offHandItemStack.getType() == Material.BARRIER || mainHandItemStack.getType() == Material.BARRIER) {
            event.setCancelled(true);
            return;
        }

        Bukkit.getScheduler().runTask(this, () -> syncPlayerTeam(player));
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        event.setKeepInventory(true);

        if (player.getPlayer() != null && player.hasPermission("invCaptive.admin")) return;

        // 방벽을 제외한 나머지를 드랍시킴
        List<ItemStack> drops = event.getDrops();
        drops.clear();

        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack != null) {
                if (itemStack.getType() != Material.BARRIER) {
                    drops.add(itemStack);
                    inventory.setItem(i, null);
                }
            }
        }

        Bukkit.getScheduler().runTask(this, () -> syncPlayerTeam(player));
    }

    @Override
    public void onDisable() {
        List<Integer> processedTeams = new ArrayList<Integer>();
        for (Player player: Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("invCaptive.admin")) continue;

            int team = teamManager.getPlayerTeam(player.getName(), this::getInventory);
            if (processedTeams.contains(team)) continue;

            String rootKey = String.format("groups.group%d.", team);
            List<ItemStack> lastInventoryData = inventoryManager.export(player);
            getInventory().set(rootKey + "lastItems", lastInventoryData);
            processedTeams.add(team);
        }

        saveInventory();

        int groupsCount = getInventory().getInt("groups.total");
        for (int i = 0; i < groupsCount; i++) {
            World world = Bukkit.getWorld("world_the_end_Group" + (i + 1));
            if (world != null) {
                world.save();
                Bukkit.unloadWorld(world, true);
            }
        }

        logger.info("Inv-Captive Disabled.");
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return commandsManager.onCommand(sender, command, label, args);
    }

    public void syncPlayerTeam(Player player) {
        int team = teamManager.getPlayerTeam(player.getName(), this::getInventory);
        List<String> teamPlayers = teamManager.getTeamPlayers(team, this::getInventory);
        for (String current: teamPlayers) {
            // 찾는 대상이 현재 플레이어 이름과 같다면 무시
            if (current.equals(player.getName().toLowerCase())) continue;

            for (Player onlinePlayer: Bukkit.getOnlinePlayers()) {
                // 찾는 대상과 온라인 대상이 같다면 적용
                if (current.equals(onlinePlayer.getName().toLowerCase())) {
                    inventoryManager.syncInventory(player, onlinePlayer);
                    break;
                }
            }
        }
    }

    public void saveInventory() {
        try {
            inventoryData.save(inventoryDataFile);
        } catch (IOException e) {
            e.printStackTrace();
            getServer().shutdown();
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
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            getServer().shutdown();
        }
    }

    public FileConfiguration getInventory() {
        if (inventoryData == null) {
            reloadInventory();
        }
        return inventoryData;
    }
}
