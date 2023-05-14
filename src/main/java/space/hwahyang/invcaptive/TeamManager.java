package space.hwahyang.invcaptive;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.function.Supplier;

public class TeamManager {

    private final Inv_Captive inv_captive;

    private final int END_ISLAND_RADIUS = 150;

    private final ItemStack DEFAULT_AIR = new ItemStack(Material.AIR, 0);
    private final ItemStack DEFAULT_BARRIER;

    public TeamManager(Inv_Captive inv_captive) {
        this.inv_captive = inv_captive;

        DEFAULT_BARRIER = new ItemStack(Material.BARRIER, 1);
        ItemMeta itemMeta = DEFAULT_BARRIER.getItemMeta();
        itemMeta.setDisplayName(inv_captive.getConfig().getString("messages.minecraft.item.locked").replace("&", "§"));
        DEFAULT_BARRIER.setItemMeta(itemMeta);
    }

    /**
     * 팀을 생성합니다.
     * @param inv_captive 활성화 된 Inv_Captive 인스턴스를 지정합니다.
     * @param players 둥록할 플레이어의 닉네임을 지정합니다.
     * @param inventoryManager InventoryManager 인스턴스를 지정합니다.
     * @return 팀명이 반환됩니다. (실패 시, -1이 반환됩니다.)
     */
    public int createTeam(Inv_Captive inv_captive, String[] players, InventoryManager inventoryManager) {
        for (String player: players) {
            int existTeam = getPlayerTeam(player, inv_captive::getInventory);
            if (existTeam != -1) return -1;
        }

        List<Material> unlockItems = inventoryManager.getMaterialArray();
        List<ItemStack> lastItems = new ArrayList<ItemStack>();
        for (int i = 0; i < inventoryManager.getInventoryMax(); i++) {
            lastItems.add(i == 0 ? DEFAULT_AIR : DEFAULT_BARRIER);
        }

        int total = inv_captive.getInventory().getInt("groups.total");
        String rootKey = String.format("groups.group%d.", total + 1);

        // 팀의 엔드 생성
        Bukkit.getScheduler().runTask(inv_captive, () -> {
            World separateWorld = null;

            do {
                WorldCreator worldCreator = new WorldCreator("world_the_end_Group" + (total + 1));
                worldCreator.environment(World.Environment.THE_END);
                worldCreator.generateStructures(true);
                separateWorld = Bukkit.createWorld(worldCreator);
            } while (separateWorld == null);

            // 일반적으로 엔드의 스폰 위치는 출구 포탈 -> 무작위 위치로 스폰지점 이동
            Random rand = new Random();
            Location endIslandCenter = new Location(separateWorld, 0, 64, 0); // center of the End Island
            double angleX = rand.nextDouble() * 2 * Math.PI;
            double angleZ = rand.nextDouble() * 2 * Math.PI;
            double distanceX = rand.nextDouble() * END_ISLAND_RADIUS;
            double distanceZ = rand.nextDouble() * END_ISLAND_RADIUS;
            double x = Math.cos(angleX) * distanceX;
            double z = Math.sin(angleZ) * distanceZ;
            Location randomLocation = endIslandCenter.clone().add(x, 0, z); // add the random x and z coordinates to the center of the End Island
            randomLocation.setY(separateWorld.getHighestBlockYAt(randomLocation)); // set the y coordinate to the height of the highest block at the location
            if (randomLocation.getY() <= 25) randomLocation.setY(64);
            separateWorld.setSpawnLocation(randomLocation);

            Bukkit.getServer().getWorlds().add(separateWorld);
        });

        inv_captive.getInventory().set(rootKey + "players", players);
        inv_captive.getInventory().set(rootKey + "unlockItems", inventoryManager.convertMaterialArrayToStringArray(unlockItems));
        inv_captive.getInventory().set(rootKey + "lastItems", lastItems);
        inv_captive.getInventory().set("groups.total", total + 1);
        inv_captive.saveInventory();
        inv_captive.reloadInventory();

        return total + 1;
    }

    /**
     * 플레이어가 소속되어 있는 팀을 조회합니다.
     * @param player 조회할 플레이어의 닉네임을 지정합니다.
     * @param getFunction inventory.yml을 조회할 수 있는(반환하는) 함수를 지정합니다.
     * @return 조회한 플레이어의 팀명이 반환됩니다. (실패 시, -1이 반환됩니다.)
     */
    public int getPlayerTeam(String player, Supplier<FileConfiguration> getFunction) {
        FileConfiguration data = getFunction.get();

        for (int i = 0; i < data.getInt("groups.total"); i++) {
            String loopKey = String.format("groups.group%d.", i + 1);
            List<String> players = data.getStringList(loopKey + "players");
            for (String current: players) {
                if (current.equals(player.toLowerCase())) return i + 1;
            }
        }

        return -1;
    }

    /**
     * 특정 팀의 팀원을 조회합니다.
     * @param name 조회할 팀의 이름을 지정합니다.
     * @param getFunction inventory.yml을 조회할 수 있는(반환하는) 함수를 지정합니다.
     * @return 조회한 팀의 팀원 정보가 반환됩니다. (없을 경우, null이 반환됩니다.)
     */
    public List<String> getTeamPlayers(int name, Supplier<FileConfiguration> getFunction) {
        FileConfiguration data = getFunction.get();

        if (name < 0 || data.getInt("groups.total") < name ) return null;

        return data.getStringList(String.format("groups.group%d.players", name));
    }

    /**
     * 특정 팀의 슬롯별 해금 아이템을 조회합니다.
     * @param name 조회할 팀의 이름을 지정합니다.
     * @param getFunction inventory.yml을 조회할 수 있는(반환하는) 함수를 지정합니다.
     * @return 조회한 팀의 슬롯별 해금 아이템이 반환됩니다. (없을 경우, null이 반환됩니다.)
     */
    public List<Material> getTeamUnlockItems(int name, Supplier<FileConfiguration> getFunction) {
        FileConfiguration data = getFunction.get();

        if (name < 0 || data.getInt("groups.total") < name ) return null;

        List<String> original = data.getStringList(String.format("groups.group%d.unlockItems", name));

        return inv_captive.getInventoryManager().convertStringArrayToMaterialArray(original);
    }

    public String getTeamInfo(String name, String baseText, Supplier<FileConfiguration> getFunction) {
        int team = getPlayerTeam(name, getFunction);
        if (team == -1) return null;
        return getTeamInfo(team, baseText, getFunction);
    }
    public String getTeamInfo(int name, String baseText, Supplier<FileConfiguration> getFunction) {
        FileConfiguration data = getFunction.get();

        if (name < 0 || data.getInt("groups.total") < name) return null;

        List<String> players = data.getStringList(String.format("groups.group%d.players", name));
        List<ItemStack> lastItems = (List<ItemStack>) data.getList(String.format("groups.group%d.lastItems", name));

        int current = 0;
        if (lastItems == null) current = inv_captive.getInventoryManager().getInventoryMax();
        else {
            for (ItemStack itemStack: lastItems) {
                if (itemStack == null || itemStack.getType() != Material.BARRIER) current++;
            }
        }

        String returnText = baseText;
        returnText = returnText.replace("{TEAM}", String.valueOf(name));
        returnText = returnText.replace("{TEAMMATE0}", players.get(0));
        returnText = returnText.replace("{TEAMMATE1}", players.size() == 1 ? "(없음)" : players.get(1));
        returnText = returnText.replace("{CURRENT}", String.valueOf(current));
        returnText = returnText.replace("{MAX}", String.valueOf(inv_captive.getInventoryManager().getInventoryMax()));
        returnText = returnText.replace("{PERCENT}", current * 1f / inv_captive.getInventoryManager().getInventoryMax() * 100 + "%");

        return returnText;
    }

    public String getTop5Teams(String baseText, Supplier<FileConfiguration> getFunction) {
        FileConfiguration data = getFunction.get();

        String returnText = baseText;
        int total = inv_captive.getInventory().getInt("groups.total");

        Map<Float, Integer> ranks = new TreeMap<Float, Integer>(Comparator.reverseOrder());
        for (int i = 0; i < total; i++) {
            int current = 0;
            List<ItemStack> lastItems = (List<ItemStack>) data.getList(String.format("groups.group%d.lastItems", i + 1));
            if (lastItems == null) current = inv_captive.getInventoryManager().getInventoryMax();
            else {
                for (ItemStack itemStack: lastItems) {
                    if (itemStack == null || itemStack.getType() != Material.BARRIER) current++;
                }
            }
            float percent = current * 1f / inv_captive.getInventoryManager().getInventoryMax() * 100;
            ranks.put(percent, i + 1);
        }

        int currentRound = 0;
        for (Map.Entry<Float, Integer> entry : ranks.entrySet()) {
            int team = entry.getValue();

            List<String> players = data.getStringList(String.format("groups.group%d.players", team));
            List<ItemStack> lastItems = (List<ItemStack>) data.getList(String.format("groups.group%d.lastItems", team));

            int current = 0;
            if (lastItems == null) current = inv_captive.getInventoryManager().getInventoryMax();
            else {
                for (ItemStack itemStack: lastItems) {
                    if (itemStack == null || itemStack.getType() != Material.BARRIER) current++;
                }
            }

            returnText = returnText.replace(String.format("{TEAM%d_NAME}", currentRound), String.valueOf(team));
            returnText = returnText.replace(String.format("{TEAM%d_MATE0}", currentRound), players.get(0));
            returnText = returnText.replace(String.format("{TEAM%d_MATE1}", currentRound), players.size() == 1 ? "(없음)" : players.get(1));
            returnText = returnText.replace(String.format("{TEAM%d_CURRENT}", currentRound), String.valueOf(current));
            returnText = returnText.replace(String.format("{TEAM%d_MAX}", currentRound), String.valueOf(inv_captive.getInventoryManager().getInventoryMax()));
            returnText = returnText.replace(String.format("{TEAM%d_PERCENT}", currentRound), entry.getKey() + "%");
            currentRound++;
            if (currentRound >= 5) break;
        }

        // 혹시 총 팀이 부족해서 5개를 다 못채운다면 나머지는 '없음'으로 도배해주기
        if (total <= 5) {
            for (int i = 0; i < 5; i++) {
                returnText = returnText.replace(String.format("{TEAM%d_NAME}", i), "(없음)");
                returnText = returnText.replace(String.format("{TEAM%d_MATE0}", i), "(없음)");
                returnText = returnText.replace(String.format("{TEAM%d_MATE1}", i), "(없음)");
                returnText = returnText.replace(String.format("{TEAM%d_CURRENT}", i), "0");
                returnText = returnText.replace(String.format("{TEAM%d_MAX}", i), String.valueOf(inv_captive.getInventoryManager().getInventoryMax()));
                returnText = returnText.replace(String.format("{TEAM%d_PERCENT}", i), "0%");
            }
        }

        return returnText;
    }
}
