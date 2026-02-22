package org.example.artyom.magicMechanism.data.enums;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.data.enums.barrier.*;

import java.util.function.Function;
import java.util.Optional;

public interface MenuAction {
    String getPdcKey();

    static MenuAction fromPDC(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return null;

        String actionStr = item.getItemMeta()
                .getPersistentDataContainer()
                .get(key, PersistentDataType.STRING);

        if (actionStr == null || actionStr.isEmpty()) return null;

        ActionPrefix prefix = ActionPrefix.fromString(actionStr);
        if (prefix == null) return null;

        return switch (prefix) {
            case BARRIER -> parseBarrierAction(actionStr);
            case PLAYERLIST -> parsePlayerListAction(actionStr);
            case PLAYERSETTINGS -> BarrierPlayerSettingsMenuActions.fromString(actionStr);
        };
    }

    private static MenuAction parseBarrierAction(String actionStr) {
        if (actionStr.endsWith("ADD_PLAYER")) {
            return BarrierMenuActions.fromString(actionStr);
        }
        return parseSlotAction(actionStr, BarrierMenuActionSlot::new);
    }

    private static MenuAction parsePlayerListAction(String actionStr) {
        if (isPaginationAction(actionStr)) {
            return BarrierPlayerListMenuActions.fromString(actionStr);
        }
        return parseSlotAction(actionStr, BarrierPlayerListMenuActionSlot::new);
    }

    private static boolean isPaginationAction(String actionStr) {
        return actionStr.endsWith("RETURN_BACK") ||
                actionStr.endsWith("NEXT_PAGE") ||
                actionStr.endsWith("PREVIOUS_PAGE");
    }

    private static MenuAction parseSlotAction(String actionStr, Function<Integer, MenuAction> constructor) {
        try {
            return extractSlot(actionStr)
                    .map(constructor)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static Optional<Integer> extractSlot(String actionStr) {
        return Optional.of(actionStr)
                .map(s -> s.split(":"))
                .filter(parts -> parts.length >= 2)
                .map(parts -> parts[1])
                .map(s -> {
                    try {
                        return Integer.parseInt(s);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                });
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
