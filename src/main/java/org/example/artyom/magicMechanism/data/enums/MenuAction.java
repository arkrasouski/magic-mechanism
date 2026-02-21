package org.example.artyom.magicMechanism.data.enums;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.data.enums.barrier.*;

public interface MenuAction {
    String getPdcKey();  // "BARRIER_MAIN_MENU_ADD_PLAYER"

    static MenuAction fromPDC(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return null;

        String actionStr = item.getItemMeta()
                .getPersistentDataContainer()
                .get(key, PersistentDataType.STRING);
        System.out.println(actionStr);
        if (actionStr == null) return null;
        // Пробуем разные enum по префиксу
        if (actionStr.startsWith("BARRIER_")) {
            if(actionStr.endsWith("ADD_PLAYER")) {
                return BarrierMenuActions.fromString(actionStr);
            }
            try {
                return new BarrierMenuActionSlot(Integer.parseInt(actionStr.split(":")[1]));
            } catch (Exception ignored) {}
        } else if (actionStr.startsWith("PLAYERLIST_")) {
            if(actionStr.endsWith("RETURN_BACK")) {
                return BarrierPlayerListMenuActions.fromString(actionStr);
            }
            try {
                return new BarrierPlayerListMenuActionSlot(Integer.parseInt(actionStr.split(":")[1]));
            } catch (Exception ignored) {}

        }
       else if (actionStr.startsWith("PLAYERSETTINGS_")) {
            return BarrierPlayerSettingsMenuActions.fromString(actionStr);
        }

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
