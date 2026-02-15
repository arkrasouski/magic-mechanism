package org.example.artyom.magicMechanism.mechanisms;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.data.Keys;

import java.util.ArrayList;
import java.util.List;


public class Generator extends BaseMechanism {

    private static final Material material = Material.DROPPER;
    private static final String key_type = "generator";
    public static final int capacity = 1000;

    public Generator() {
        super(material, "Энерго-генератор", key_type, "Ваш генератор", capacity, 10, 20);
    }


    static final BlockFace[] FACES_6 = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
    };

    public static boolean isGenerator(Block block){
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

    public static  List<Block> adjacentMechanisms(Block generator) {
        List<Block> res = new ArrayList<>();
        for (BlockFace f : FACES_6) {
            Block b = generator.getRelative(f);
            if (Barrier.isBarrier(b)) res.add(b);
        }
        return res;
    }
}
