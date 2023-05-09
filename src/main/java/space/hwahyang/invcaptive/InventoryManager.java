package space.hwahyang.invcaptive;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.stream.Collectors;

public class InventoryManager {

    private int inventoryMax = 9 * 4 + 5; // 플레이어 인벤토리 36칸 + 장비 4칸 + 보조손 1칸 = 41칸
    public int getInventoryMax() {
        return inventoryMax;
    }

    /**
     * 배치 가능한 Material을 무작위 배열로 반환합니다.
     * @return 배치 가능한 모든 Material이 Inventory 크기에 맞춰 무작위로 섞여서 반환됩니다.
     */
    public List<Material> getMaterialArray() {
        List<Material> data = Arrays.stream(Material.values()).filter(target -> target.isBlock() && !target.isAir()).collect(Collectors.toList());
        Collections.shuffle(data);

        data = data.subList(0, inventoryMax);

        return data;
    }

    /**
     * 플레이어의 인벤토리를 새로 지정합니다.
     * @param player 대상 플레이어를 지정합니다.
     * @param inventory 새로 지정할 인벤토리를 지정합니다.
     * @remarks 인벤토리 코드는 ./invCodes.png를 참고하세요.
     */
    public void apply(Player player, List<ItemStack> inventory) {
        for (int i = 0; i < inventoryMax; i++) {
            player.getInventory().setItem(i, new ItemStack(inventory.get(i)));
        }
    }

    /**
     * 플레이어의 인벤토리를 반환합니다.
     * @param player 대상 플레이어를 지정합니다.
     * @return 대상 플레이어의 인벤토리가 반환됩니다.
     * @remarks 인벤토리 코드는 ./invCodes.png를 참고하세요.
     */
    public List<ItemStack> export(Player player) {
        List<ItemStack> data = new ArrayList<ItemStack>();
        for (int i = 0; i < inventoryMax; i++) {
            data.add(player.getInventory().getItem(i));
        }
        return data;
    }

    /* =========================== YAML <-> Code Convert =========================== */

    public List<String> convertMaterialArrayToStringArray(List<Material> original) {
        List<String> data = new ArrayList<String>();

        for (Material material: original) {
            data.add(material.name());
        }

        return data;
    }

    public List<Material> convertStringArrayToMaterialArray(List<String> original) {
        List<Material> data = new ArrayList<Material>();

        for (String name: original) {
            data.add(Material.getMaterial(name));
        }

        return data;
    }

    public SimpleEntry<List<String>, List<Integer>> convertItemStackArrayToStringPair(List<ItemStack> original) {
        SimpleEntry<List<String>, List<Integer>> data = new SimpleEntry<List<String>, List<Integer>>(new ArrayList<String>(), new ArrayList<Integer>());

        for (ItemStack itemStack: original) {
            data.getKey().add(itemStack.getType().name());
            data.getValue().add(itemStack.getAmount());
        }

        return data;
    }

    public List<ItemStack> convertStringPairToItemStackArray(List<String> stringList, List<Integer> integerList) {
        return convertStringPairToItemStackArray(new SimpleEntry<List<String>, List<Integer>>(stringList, integerList));
    }
    public List<ItemStack> convertStringPairToItemStackArray(SimpleEntry<List<String>, List<Integer>> original) {
        List<ItemStack> data = new ArrayList<ItemStack>();

        for (int i = 0; i < original.getKey().size(); i++) {
            data.add(new ItemStack(Material.getMaterial(original.getKey().get(i)), original.getValue().get(i)));
        }

        return data;
    }
}
