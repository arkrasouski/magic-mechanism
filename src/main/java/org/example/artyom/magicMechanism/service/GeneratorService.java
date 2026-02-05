package org.example.artyom.magicMechanism.service;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.TileState;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.Keys;
import org.example.artyom.magicMechanism.utils.EnergyCellUtil;
import org.example.artyom.magicMechanism.utils.GeneratorUtil;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GeneratorService {
    private GeneratorService() {}

    // список активных трансформаторов (где есть батарейка)
    private static final Set<Location> ACTIVE = ConcurrentHashMap.newKeySet();

    public static void onCellInserted(Location loc) {
        ACTIVE.add(loc);
    }

    public static void onCellRemoved(Location loc) {
        ACTIVE.remove(loc);
    }

    // вызывать раз в тик или раз в 20 тиков из BukkitRunnable
    public static void tickAll() {
        ACTIVE.removeIf(loc -> !tickOne(loc, new GeneratorUtil()));
    }

    // true = оставить активным, false = убрать из ACTIVE
    private static boolean tickOne(Location loc, GeneratorUtil generator) {
        World w = loc.getWorld();
        if (w == null) return false;

        Block b = w.getBlockAt(loc);
        if (b.getType() != generator.getMaterial()) return false;

        if (!(b.getState() instanceof TileState tile)) return false;
        if (!generator.isMechanismBlock(b)) return false; // твой PDC machine_type

        Inventory inv = ((Container) tile).getSnapshotInventory();
        ItemStack cell = inv.getItem(0);
        if (!EnergyCellUtil.isEnergyCell(cell)) return false;

        int cellEnergy = EnergyCellUtil.getEnergy(cell);
        if (cellEnergy <= 0) return true; // батарейка стоит, но пустая

        int buf = tile.getPersistentDataContainer().getOrDefault(Keys.BUFFER, PersistentDataType.INTEGER, 0);
        int bufMax = generator.getCapacity();
        int movePerTick = 2;

        int moved = Math.min(movePerTick, cellEnergy);
        moved = Math.min(moved, bufMax - buf);
        if (moved <= 0) return true;

        // списали с батарейки
        int before = EnergyCellUtil.getEnergy(cell);
        EnergyCellUtil.setEnergy(cell, before - moved);
        inv.setItem(0, cell);
       // long after = EnergyCellUtil.getEnergy(inv.getItem(0));
        //System.out.println("before=" + before + " moved=" + moved + " after=" + after);

        // добавили в трансформатор
        tile.getPersistentDataContainer().set(Keys.BUFFER, PersistentDataType.INTEGER, buf + moved);
        tile.update(true); // сохранить TileState/PDC в мир [web:1]

        return true;
    }
}
