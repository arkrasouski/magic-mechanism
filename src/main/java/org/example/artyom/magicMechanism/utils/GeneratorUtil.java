package org.example.artyom.magicMechanism.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.MagicMechanism;

import java.util.ArrayList;
import java.util.Arrays;

public class GeneratorUtil {
    private final static NamespacedKey MECHANISM_CLASS_KEY = new NamespacedKey(MagicMechanism.getInstance(), "mechanism_item");

    public static boolean isGenerator(Block block){
        if (block == null) return false;

        // Проверяем базовый тип блока
        if (block.getType() != Material.DROPPER) return false;

        BlockState state = block.getState();

        // Проверяем, что блок поддерживает PDC
        if (!(state instanceof TileState tile)) return false;

        PersistentDataContainer pdc = tile.getPersistentDataContainer();

        // Проверяем наш ключ
        String type = pdc.get(MECHANISM_CLASS_KEY , PersistentDataType.STRING);

        return "energy_transformer".equals(type);
    }

    public static Block createBlock(Player player,
                               String displayName, String menu_class,
                               String lore1) {
        Block block = player.getLocation().getBlock();
        block.setType(Material.DROPPER);

        BlockState state = block.getState();
        if (state instanceof TileState tileState) {
            tileState.getPersistentDataContainer().set(
                    MECHANISM_CLASS_KEY,
                    PersistentDataType.STRING,
                    "energy_transformer"
            );
            tileState.update();
        }
        return block;
    }

    public static ItemStack createItem( String displayName, String lore1) {
        ItemStack item = new ItemStack(Material.DROPPER, 1);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§bЭнерго трансформатор");
        meta.getPersistentDataContainer().set(
                MECHANISM_CLASS_KEY,
                PersistentDataType.STRING,
                "energy_transformer"
        );
        meta.setLore(Arrays.asList(lore1));
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isGeneratorItem(ItemStack item){
        if (item == null) return false;

        if(item.getType() != Material.DROPPER) return false;
        // Проверяем базовый тип блока

        ItemMeta meta = item.getItemMeta();

        // Проверяем, что блок поддерживает PDC
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // Проверяем наш ключ
        String type = pdc.get(MECHANISM_CLASS_KEY , PersistentDataType.STRING);

        return "energy_transformer".equals(type);
    }

    public static void markAsEnergyTransformer(Block block) {
        if (block == null) return;

        // Убеждаемся, что это нужный базовый блок
        if (block.getType() != Material.DROPPER) {
            block.setType(Material.DROPPER);
        }

        BlockState state = block.getState();


        if (!(state instanceof TileState tile)) return;

        PersistentDataContainer pdc = tile.getPersistentDataContainer();

        // Помечаем блок как наш механизм
        pdc.set(
                MECHANISM_CLASS_KEY,
                PersistentDataType.STRING,
                "energy_transformer"
        );

        // Начальные данные (по желанию)
//        pdc.set(
//                new NamespacedKey(plugin, "energy"),
//                PersistentDataType.INTEGER,
//                0
//        );
//
//        pdc.set(
//                new NamespacedKey(plugin, "frequency"),
//                PersistentDataType.INTEGER,
//                60
//        );

        tile.update(); // ОБЯЗАТЕЛЬНО
    }
}
