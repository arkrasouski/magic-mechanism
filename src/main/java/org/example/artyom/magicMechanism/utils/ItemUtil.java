package org.example.artyom.magicMechanism.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.MagicMechanism;

import java.util.ArrayList;

public class ItemUtil {


//    //Вспомогательный класс для работы с предметами
//    public static ItemStack create(Material material, int amount, String displayName, String item_class, String lore1, String lore2, String lore3, String lore4) {
//        //Создаем кастомный предмет
//        ItemStack item = new ItemStack(material, amount);
//        ItemMeta meta = item.getItemMeta();
//        assert meta != null;
//        if (displayName != null) {
//
//            meta.setDisplayName(displayName);
//        }
//        ArrayList<String> lore = new ArrayList<>();
//
//        String[] lores = {lore1, lore2, lore3, lore4};
//
//        for (String l : lores) {
//            if (l != null) {
//                lore.add(l);
//            }
//        }
//
//        meta.getPersistentDataContainer().set(
//                MECHANISM_CLASS_KEY,
//                PersistentDataType.STRING,
//                item_class
//        );
//
//        item.setItemMeta(meta);
//
//        meta.setLore(lore);
//        item.setItemMeta(meta);
//        return item;
//    }
//
//    public static ItemStack create(Material material, int amount, String displayName, String menu_class, String lore1) {
//        return create(material, amount, displayName, menu_class, lore1, null, null, null);
//    }
//
//    public static ItemStack create(Material material, String displayName, String menu_class) {
//        return create(material, 1, displayName, menu_class, null, null, null, null);
//    }




}