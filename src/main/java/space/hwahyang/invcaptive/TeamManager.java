package space.hwahyang.invcaptive;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.AbstractMap.SimpleEntry;
import java.util.function.Supplier;

public class TeamManager {

    /**
     * 팀을 생성합니다.
     * @param inv_captive 활성화 된 Inv_Captive 인스턴스를 지정합니다.
     * @param players 둥록할 플레이어의 닉네임을 지정합니다.
     * @param inventoryManager InventoryManager 인스턴스를 지정합니다.
     * @return 팀명이 반환됩니다. (실패 시, -1이 반환됩니다.)
     */
    public int createTeam(Inv_Captive inv_captive, String[] players, InventoryManager inventoryManager) {
        for (String player: players) {
            int existTeam = getPlayerTeam(player, inv_captive::getInventory, inv_captive);
            if (existTeam != -1) return -1;
        }

        List<Material> unlockItems = inventoryManager.getMaterialArray();
        List<ItemStack> lastItems = new ArrayList<ItemStack>();
        for (int i = 0; i < inventoryManager.getInventoryMax(); i++) {
            lastItems.add(new ItemStack(i == 0 ? Material.AIR : Material.BARRIER, 1));
        }

        int total = inv_captive.getInventory().getInt("groups.total");
        SimpleEntry<List<String>, List<Integer>> lastItemsConverted = inventoryManager.convertItemStackArrayToStringPair(lastItems);
        String loopKey = String.format("groups.group%d.", total + 1);

        inv_captive.getInventory().set(loopKey + "players", players);
        inv_captive.getInventory().set(loopKey + "unlockItems", inventoryManager.convertMaterialArrayToStringArray(unlockItems));
        inv_captive.getInventory().set(loopKey + "lastItems.codes", lastItemsConverted.getKey());
        inv_captive.getInventory().set(loopKey + "lastItems.counts", lastItemsConverted.getValue());
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
    public int getPlayerTeam(String player, Supplier<FileConfiguration> getFunction, Inv_Captive inv_captive) {
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
     * 특정 팀을 조회합니다.
     * @param name 조회할 팀의 이름을 지정합니다.
     * @param getFunction inventory.yml을 조회할 수 있는(반환하는) 함수를 지정합니다.
     * @return 조회한 팀의 정보가 반환됩니다. (없을 경우, null이 반환됩니다.)
     */
    public String getTeamInfo(String name, Supplier<FileConfiguration> getFunction) {
        // TODO
        return null;
    }
}
