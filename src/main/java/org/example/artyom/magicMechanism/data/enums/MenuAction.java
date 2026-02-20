package org.example.artyom.magicMechanism.data.enums;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public interface MenuAction {
    String getPdcKey();  // "BARRIER_MAIN_MENU_ADD_PLAYER"

    static MenuAction fromPDC(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return null;
        System.out.println("pdc");
        String actionStr = item.getItemMeta()
                .getPersistentDataContainer()
                .get(key, PersistentDataType.STRING);

        if (actionStr == null) return null;
        System.out.println("is not null: " + actionStr);
        System.out.println(actionStr.replaceAll("_\\d+$", ""));
        // Пробуем разные enum по префиксу
        if (actionStr.startsWith("BARRIER_")) {
            try {
                return BarrierMenuActions.fromString(actionStr.replaceAll("_\\d+$", ""));
            } catch (Exception ignored) {}
        } else if (actionStr.startsWith("PLAYERLIST_")) {
            return BarrierPlayerListMenuActions.fromString(actionStr);
        }
        // Другие enum...

        return null;
    }

    // Вспомогательный метод для каждого enum
    static <T extends Enum<T> & MenuAction> T fromString(String fullName, Class<T> enumClass) {
        try {
            String simpleName = fullName.split("_", 2)[1]; // убираем BARRIER_
            return Enum.valueOf(enumClass, simpleName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
