package space.hwahyang.invcaptive;

import org.bukkit.Material;

import java.util.*;

public class InventoryManager {

    /**
     * 배치 가능한 Material을 무작위 배열로 반환합니다.
     * @return 배치 가능한 모든 Material이 Inventory 크기에 맞춰 무작위로 섞여서 반환됩니다.
     */
    public EnumMap<Material, Integer> getMaterialArray() {
        List<Material> data = Arrays.stream(Material.values()).filter(target -> target.isBlock() && !target.isAir()).toList();
        Collections.shuffle(data);

        int count = 9 * 4 + 5; // 플레이어 인벤토리 36칸 + 장비 4칸 + 보조손 1칸
        EnumMap<Material, Integer> map = new EnumMap<>(Material.class);

        for (int i = 0; i < count; i++) {
            map.put(data.get(i), i);
        }

        return map;
    }
}
