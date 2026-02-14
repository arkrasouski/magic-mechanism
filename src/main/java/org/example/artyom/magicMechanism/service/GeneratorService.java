package org.example.artyom.magicMechanism.service;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.Keys;
import org.example.artyom.magicMechanism.data.BlockPosKey;
import org.example.artyom.magicMechanism.data.GeneratorGuiManager;
import org.example.artyom.magicMechanism.inventories.GenHolder;
import org.example.artyom.magicMechanism.inventories.GenStorage;
import org.example.artyom.magicMechanism.utils.EnergyCellUtil;
import org.example.artyom.magicMechanism.utils.GeneratorUtil;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class GeneratorService {
    private final GeneratorGuiManager guiManager;
    public GeneratorService(GeneratorGuiManager guiManager) {
        this.guiManager = guiManager;
    }

    private static final Set<Location> ACTIVE = ConcurrentHashMap.newKeySet();

    public static void onCellInserted(Location loc) {
        ACTIVE.add(loc);
    }

    public static void onCellRemoved(Location loc) {
        ACTIVE.remove(loc);
    }

    public boolean hasActive() {
        return !ACTIVE.isEmpty();
    }
    // вызывать раз в тик/20 тиков
    public void tickAll() {
        ACTIVE.removeIf(loc -> !tickOne(loc));
    }

    public void tickOpenGuis() {
        guiManager.viewers().forEach((k, uuids) -> {
            World w = Bukkit.getWorld(k.worldId()); // [web:245]
            if (w == null) return;

            Block b = w.getBlockAt(k.x(), k.y(), k.z()); // [web:341]
            if (!GeneratorUtil.isGenerator(b)) return;

            BlockState st = b.getState();
            if (!(st instanceof TileState tile)) return;

            // истина из PDC
            Inventory tmp = Bukkit.createInventory(null, 27);
            GenStorage.loadItems(tile, tmp);
            ItemStack cellFromPdc = tmp.getItem(0);

            uuids.removeIf(uuid -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null) return true; // оффлайн [web:331]

                Inventory top = p.getOpenInventory().getTopInventory(); // верхний инвентарь окна [web:239]
                if (!(top.getHolder() instanceof GenHolder h)) return true;

                // защита: игрок мог открыть другой генератор
                if (!BlockPosKey.of(h.getLocation()).equals(k)) return true;

                // UI-обновление (при желании добавь фильтр, чтобы не перетирать игрока)
                top.setItem(0, cellFromPdc);
                return false;
            });
        });
    }

    // true = оставить активным, false = убрать
    private boolean tickOne(Location loc) {
        World w = loc.getWorld();
        if (w == null) return false;

        Block b = w.getBlockAt(loc);
        if (!GeneratorUtil.isGenerator(b)) return false;

        BlockState st = b.getState();
        if (!(st instanceof TileState tile)) return false;

        Inventory inv = Bukkit.createInventory(null, 27);
        GenStorage.loadItems(tile, inv);

        ItemStack cell = inv.getItem(0);
        if (!EnergyCellUtil.isEnergyCell(cell)) return false;

        int cellEnergy = EnergyCellUtil.getEnergy(cell);
        if (cellEnergy <= 0) return true;

        PersistentDataContainer pdc = tile.getPersistentDataContainer();
        int buf = pdc.getOrDefault(Keys.BUFFER, PersistentDataType.INTEGER, 0);

        int moved = Math.min(2, cellEnergy);
        moved = Math.min(moved, GeneratorUtil.capacity - buf);
        if (moved <= 0) return true;

        // Меняем ТОЛЬКО здесь
        EnergyCellUtil.setEnergy(cell, cellEnergy - moved); // внутри обновится lore
        inv.setItem(0, cell);

        pdc.set(Keys.BUFFER, PersistentDataType.INTEGER, buf + moved);

        GenStorage.saveItems(tile, inv); // внутри tile.update() обязательно [web:65]
        return true;
    }

}