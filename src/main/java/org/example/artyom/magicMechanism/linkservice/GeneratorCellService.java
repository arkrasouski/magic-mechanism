package org.example.artyom.magicMechanism.linkservice;

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
import org.example.artyom.magicMechanism.data.Keys;
import org.example.artyom.magicMechanism.data.records.BlockPosKey;
import org.example.artyom.magicMechanism.data.GeneratorGuiManager;
import org.example.artyom.magicMechanism.inventories.GenInventory;
import org.example.artyom.magicMechanism.inventories.MechanismHolder;
import org.example.artyom.magicMechanism.inventories.MechanismStorage;
import org.example.artyom.magicMechanism.energyitems.EnergyCell;
import org.example.artyom.magicMechanism.mechanisms.Generator;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class GeneratorCellService {
    private final GeneratorGuiManager guiManager;
    private final GenInventory genInventory;

    public GeneratorCellService(GeneratorGuiManager guiManager, GenInventory genInventory) {
        this.guiManager = guiManager;
        this.genInventory = genInventory;
    }
    private final Map<BlockPosKey, Generator> generators = new HashMap<>();

    private static final Set<Location> ACTIVE = ConcurrentHashMap.newKeySet();


    public void registerGenerator(Block b) {
        generators.put(BlockPosKey.of(b.getLocation()), new Generator());
    }

    public void unregisterGenerator(Block b) {
        generators.remove(BlockPosKey.of(b.getLocation()));
    }

    public Collection<Map.Entry<BlockPosKey, Generator>> allGenerators() {
        return generators.entrySet();
    }

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
            if (!Generator.isGenerator(b)) return;

            BlockState st = b.getState();
            if (!(st instanceof TileState tile)) return;

            // истина из PDC
            Inventory tmp = Bukkit.createInventory(null, 27);
            MechanismStorage.loadItems(tile, tmp);
            ItemStack cellFromPdc = tmp.getItem(9);

            uuids.removeIf(uuid -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null) return true; // оффлайн [web:331]

                Inventory top = p.getOpenInventory().getTopInventory(); // верхний инвентарь окна [web:239]
                if (!(top.getHolder() instanceof MechanismHolder h)) return true;

                // защита: игрок мог открыть другой генератор
                if (!BlockPosKey.of(h.getLocation()).equals(k)) return true;
                PersistentDataContainer genPDC = tile.getPersistentDataContainer();
                int currentEnergy = genPDC.getOrDefault(Keys.BUFFER, PersistentDataType.INTEGER, 0);
                int capacity = genPDC.getOrDefault(Keys.CAPACITY, PersistentDataType.INTEGER, 0);
                genInventory.updateEnergyBar(top, (double) (currentEnergy * 100) / capacity);

                // UI-обновление (при желании добавь фильтр, чтобы не перетирать игрока)
                if(!EnergyCell.isEnergyCell(cellFromPdc)) return false;
                top.setItem(9, cellFromPdc);
                return false;
            });
        });
    }

    // true = оставить активным, false = убрать
    private boolean tickOne(Location loc) {
        World w = loc.getWorld();
        if (w == null) return false;

        Block b = w.getBlockAt(loc);
        if (!Generator.isGenerator(b)) return false;

        BlockState st = b.getState();
        if (!(st instanceof TileState tile)) return false;

        Inventory inv = Bukkit.createInventory(null, 27);
        MechanismStorage.loadItems(tile, inv);

        ItemStack cell = inv.getItem(9);
        if (!EnergyCell.isEnergyCell(cell)) return false;

        int cellEnergy = EnergyCell.getEnergy(cell);
        if (cellEnergy <= 0) return true;

        PersistentDataContainer pdc = tile.getPersistentDataContainer();
        int buf = pdc.getOrDefault(Keys.BUFFER, PersistentDataType.INTEGER, 0);

        int moved = Math.min(2, cellEnergy);
        moved = Math.min(moved, Generator.capacity - buf);
        if (moved <= 0) return true;

        // Меняем ТОЛЬКО здесь
        EnergyCell.setEnergy(cell, cellEnergy - moved); // внутри обновится lore
        inv.setItem(9, cell);

        pdc.set(Keys.BUFFER, PersistentDataType.INTEGER, buf + moved);

        MechanismStorage.saveItems(tile, inv); // внутри tile.update() обязательно [web:65]
        return true;
    }

}