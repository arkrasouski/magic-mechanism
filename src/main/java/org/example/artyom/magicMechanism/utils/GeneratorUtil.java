package org.example.artyom.magicMechanism.utils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.Keys;


public class GeneratorUtil extends BaseMechanismUtil {

    private static final Material material = Material.DROPPER;
    private static final String key_type = "generator";
    public static final int capacity = 1000;

    public GeneratorUtil() {
        super(material, "Энерго-генератор", key_type, "Ваш генератор", capacity, 10);
    }

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
}
