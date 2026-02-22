//package org.example.artyom.magicMechanism.mechanisms;
//
//import org.bukkit.Material;
//import org.bukkit.block.Block;
//import org.bukkit.block.BlockState;
//import org.bukkit.block.TileState;
//import org.bukkit.persistence.PersistentDataContainer;
//import org.bukkit.persistence.PersistentDataType;
//import org.example.artyom.magicMechanism.data.Keys;
//
//public class Barrier extends BaseMechanism {
//    private static final Material material = Material.BARREL;
//    private static final String key_type = "barrier";
//    public static final int capacity = 750;
//public Barrier() {
//    super(material, "Энерго-барьер", key_type, "Ваш барьер", capacity, 10, 60*60*20);
//}
//
//    public static boolean isBarrier(Block block){
//        if (block == null) return false;
//
//        // Проверяем базовый тип блока
//        if (block.getType() != material) return false;
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
//        return key_type.equals(type);
//    }
//
//}