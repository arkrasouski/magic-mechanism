package org.example.artyom.magicMechanism.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.example.artyom.magicMechanism.Keys;
import org.example.artyom.magicMechanism.MagicMechanism;

import java.util.ArrayList;
import java.util.Arrays;


public class GeneratorUtil extends BaseMechanismUtil {
    public GeneratorUtil() {
        super(Material.DROPPER, "Энерго-генератор", "generator", "Ваш генератор", 1000, 10);
    }

    public GeneratorUtil(Material material, String name, String key_type, String lore, int capacity, int frequency) {
        super(material, name, key_type, lore, capacity, frequency);
    }


}

//public class GeneratorUtil {
//
//    public static boolean isGenerator(Block block){
//        if (block == null) return false;
//
//        // Проверяем базовый тип блока
//        if (block.getType() != Material.DROPPER) return false;
//
//        BlockState state = block.getState();
//
//        // Проверяем, что блок поддерживает PDC
//        if (!(state instanceof TileState tile)) return false;
//
//        PersistentDataContainer pdc = tile.getPersistentDataContainer();
//
//        // Проверяем наш ключ
//        String type = pdc.get(Keys.MACHINE_TYPE , PersistentDataType.STRING);
//
//        return "energy_transformer".equals(type);
//    }
//
////    public static Block createBlock(Player player,
////                               String displayName, String menu_class,
////                               String lore1) {
////        Block block = player.getLocation().getBlock();
////        block.setType(Material.DROPPER);
////
////        BlockState state = block.getState();
////        if (state instanceof TileState tileState) {
////            tileState.getPersistentDataContainer().set(
////                    Keys.MACHINE_TYPE,
////                    PersistentDataType.STRING,
////                    "energy_transformer"
////            );
////            tileState.update();
////        }
////        return block;
////    }
//
//    public static ItemStack createGenerator() {
//        ItemStack item = new ItemStack(Material.DROPPER, 1);
//        ItemMeta meta = item.getItemMeta();
//
//        meta.setDisplayName("§bЭнерго трансформатор");
//        meta.getPersistentDataContainer().set(
//                Keys.MACHINE_TYPE,
//                PersistentDataType.STRING,
//                "energy_transformer"
//        );
//        //meta.setCustomModelData(1001);
//        meta.setLore(Arrays.asList("Ваш генератор"));
//        item.setItemMeta(meta);
//        return item;
//    }
//
//    public static boolean isGeneratorItem(ItemStack item){
//        if (item == null) return false;
//
//        if(item.getType() != Material.DROPPER) return false;
//        // Проверяем базовый тип блока
//
//        ItemMeta meta = item.getItemMeta();
//
//        // Проверяем, что блок поддерживает PDC
//        PersistentDataContainer pdc = meta.getPersistentDataContainer();
//
//        // Проверяем наш ключ
//        String type = pdc.get(Keys.MACHINE_TYPE , PersistentDataType.STRING);
//
//        return "energy_transformer".equals(type);
//    }
//
//    public static void setEnergyTransformer(Block block) {
//        if (block == null) return;
//
//        // Убеждаемся, что это нужный базовый блок
//        if (block.getType() != Material.DROPPER) {
//            block.setType(Material.DROPPER);
//        }
//
//        BlockState state = block.getState();
//
//
//        if (!(state instanceof TileState tile)) return;
//
//        PersistentDataContainer pdc = tile.getPersistentDataContainer();
//
//        // Помечаем блок как наш механизм
//        pdc.set(
//                Keys.MACHINE_TYPE,
//                PersistentDataType.STRING,
//                "energy_transformer"
//        );
//        pdc.set(Keys.BUFFER,  PersistentDataType.LONG, 0L);
//        pdc.set(Keys.FREQ, PersistentDataType.INTEGER, 5);
//
//        //Location loc = block.getLocation().add(0.5, 0, 0.5);
//        //ItemDisplay display = (ItemDisplay) block.getWorld().spawnEntity(loc, EntityType.ITEM_DISPLAY);
//        //display.setItemStack(GeneratorUtil.createGenerator()); // с CustomModelData
//        //display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
//        //display.setBrightness(new Display.Brightness(15, 15));
//
//
//        //pdc.set(ENTITY_ID_KEY, PersistentDataType.INTEGER, (int)display.getEntityId());
//        tile.update();
//        // Начальные данные (по желанию)
////        pdc.set(
////                new NamespacedKey(plugin, "energy"),
////                PersistentDataType.INTEGER,
////                0
////        );
////
////        pdc.set(
////                new NamespacedKey(plugin, "frequency"),
////                PersistentDataType.INTEGER,
////                60
////        );
//    }
//
//    public static String getGeneratorType(TileState tile) {
//        return tile.getPersistentDataContainer().get(Keys.MACHINE_TYPE, PersistentDataType.STRING);
//    }
//}
