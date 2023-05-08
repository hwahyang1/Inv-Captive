package space.hwahyang.invcaptive;

import org.bukkit.Material;

import java.util.*;

public class InventoryManager {

    /**
     * ��ġ ������ Material�� ������ �迭�� ��ȯ�մϴ�.
     * @return ��ġ ������ ��� Material�� Inventory ũ�⿡ ���� �������� ������ ��ȯ�˴ϴ�.
     */
    public EnumMap<Material, Integer> getMaterialArray() {
        List<Material> data = Arrays.stream(Material.values()).filter(target -> target.isBlock() && !target.isAir()).toList();
        Collections.shuffle(data);

        int count = 9 * 4 + 5; // �÷��̾� �κ��丮 36ĭ + ��� 4ĭ + ������ 1ĭ
        EnumMap<Material, Integer> map = new EnumMap<>(Material.class);

        for (int i = 0; i < count; i++) {
            map.put(data.get(i), i);
        }

        return map;
    }
}
