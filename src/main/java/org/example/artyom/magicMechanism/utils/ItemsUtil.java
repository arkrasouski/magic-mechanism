package org.example.artyom.magicMechanism.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.data.Keys;

import javax.annotation.Nullable;
import java.util.List;


public class ItemsUtil {
    //Вспомогательный класс для работы с предметами
    public static ItemStack create(Material material, int amount, String displayName, String menu_class, @Nullable List<String> lore) {
        //Создаем кастомный предмет
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(displayName);
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(
                Keys.INVENTORY_ITEM,
                PersistentDataType.STRING,
                menu_class
        );
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack create(Material material, int amount, String displayName, String menu_class) {
        return create(material, amount, displayName, menu_class, null);
    }
}
