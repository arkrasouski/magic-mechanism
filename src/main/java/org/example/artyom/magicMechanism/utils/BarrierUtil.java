package org.example.artyom.magicMechanism.utils;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.Keys;

//
//import org.bukkit.Location;
//import org.bukkit.Material;
//import org.bukkit.block.Block;
//import org.bukkit.block.BlockState;
//import org.bukkit.block.TileState;
//import org.bukkit.entity.Player;
//import org.bukkit.inventory.ItemStack;
//import org.bukkit.inventory.meta.ItemMeta;
//import org.bukkit.persistence.PersistentDataContainer;
//import org.bukkit.persistence.PersistentDataType;
//import org.example.artyom.magicMechanism.Keys;
//
//import java.util.Arrays;
//
//public class BarrierUtil {
//    static boolean inSquare(Location center, Location loc, int radius) { // 5x5 => r=2 | 15x15 => r=7
//        if (!center.getWorld().equals(loc.getWorld())) return false;
//        int cx = center.getBlockX(), cz = center.getBlockZ();
//        int x = loc.getBlockX(), z = loc.getBlockZ();
//        return Math.abs(x - cx) <= radius && Math.abs(z - cz) <= radius;
//    }
//
//    boolean isDenied(Player player, Location actionLoc) {
//        BarrierInstance bi = findBarrierCovering(actionLoc); // верни активный барьер, который накрывает точку, или null
//        if (bi == null || !bi.active) return false;
//        return !player.getUniqueId().equals(bi.owner); // запрещаем только чужим
//    }
//
//    public static ItemStack createBarrier() {
//        ItemStack item = new ItemStack(Material.BARREL, 1);
//        ItemMeta meta = item.getItemMeta();
//
//        meta.setDisplayName("§bЗащитный барьер");
//        meta.getPersistentDataContainer().set(
//                Keys.BARRIER_TYPE,
//                PersistentDataType.STRING,
//                "energy_barrier"
//        );
//        //meta.setCustomModelData(1001);
//        meta.setLore(Arrays.asList("Ваш барьер"));
//        item.setItemMeta(meta);
//        return item;
//    }
//
//    public static void setEnergyBarrier(Block block) {
//        if (block == null) return;
//
//        // Убеждаемся, что это нужный базовый блок
//        if (block.getType() != Material.BARREL) {
//            block.setType(Material.BARREL);
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
//                Keys.BARRIER_TYPE,
//                PersistentDataType.STRING,
//                "energy_barrier"
//        );
//        pdc.set(Keys.BARRIER_BUFFER,  PersistentDataType.LONG, 0L);
//        pdc.set(Keys.BARRIER_FREQ, PersistentDataType.INTEGER, 10);
//
//        tile.update();
//
//    }
//
//    public static boolean isBarrierItem(ItemStack item){
//        if (item == null) return false;
//
//        if(item.getType() != Material.BARREL) return false;
//        // Проверяем базовый тип блока
//
//        ItemMeta meta = item.getItemMeta();
//
//        // Проверяем, что блок поддерживает PDC
//        PersistentDataContainer pdc = meta.getPersistentDataContainer();
//
//        // Проверяем наш ключ
//        String type = pdc.get(Keys.BARRIER_TYPE , PersistentDataType.STRING);
//
//        return "energy_barrier".equals(type);
//    }
//}
public class BarrierUtil extends BaseMechanismUtil{
    private static final Material material = Material.BARREL;
    private static final String key_type = "barrier";
    public static final int capacity = 750;
public BarrierUtil() {
    super(material, "Энерго-барьер", key_type, "Ваш барьер", capacity, 10, 60*60*20);
}

    public static boolean isBarrier(Block block){
        if (block == null) return false;

        // Проверяем базовый тип блока
        if (block.getType() != material) return false;

        BlockState state = block.getState();

        // Проверяем, что блок поддерживает PDC
        if (!(state instanceof TileState tile)) return false;

        PersistentDataContainer pdc = tile.getPersistentDataContainer();

        // Проверяем наш ключ
        String type = pdc.get(Keys.MACHINE_TYPE , PersistentDataType.STRING);

        return key_type.equals(type);
    }

}