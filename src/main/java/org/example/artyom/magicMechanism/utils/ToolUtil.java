package org.example.artyom.magicMechanism.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ToolUtil {
    public static boolean isPickaxe(ItemStack item) {
        if (item == null) return false;
        if (item.getType() == Material.AIR) return false;

        return switch (item.getType()) {
            case WOODEN_PICKAXE,
                 STONE_PICKAXE,
                 IRON_PICKAXE,
                 GOLDEN_PICKAXE,
                 DIAMOND_PICKAXE,
                 NETHERITE_PICKAXE -> true;
            default -> false;
        };
    }
}
