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
import org.example.artyom.magicMechanism.inventories.generator.GenInventory;
import org.example.artyom.magicMechanism.inventories.MechanismHolder;
import org.example.artyom.magicMechanism.inventories.MechanismStorage;
import org.example.artyom.magicMechanism.energyitems.EnergyCell;
import org.example.artyom.magicMechanism.managers.GeneratorManager;
import org.example.artyom.magicMechanism.mechanisms.Generator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class GeneratorCellService {
    private final GeneratorGuiManager guiManager;
    private final GeneratorManager generatorManager;
    private final GenInventory genInventory;
    private static final int UPDATE_INTERVAL = 20; // обновлять GUI каждые 20 тиков (1 сек)
    private static final int CELL_SLOT = 9;
    private int guiUpdateCounter = 0;
    public GeneratorCellService(GeneratorGuiManager guiManager, GeneratorManager generatorManager) {
        this.guiManager = guiManager;
        this.generatorManager = generatorManager;
        this.genInventory = new GenInventory();
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

    // ========== Основной тикер ==========
    public void tickAll() {
        ACTIVE.removeIf(loc -> !tickOne(loc));

        guiUpdateCounter++;
        if (guiUpdateCounter >= UPDATE_INTERVAL) {
            tickOpenGuis();
            guiUpdateCounter = 0;
        }
    }



    public void tickOpenGuis() {
        guiManager.viewers().forEach((blockPos, playerUuids) -> {
            World w = Bukkit.getWorld(blockPos.worldId());
            if (w == null) return;

            Block block = w.getBlockAt(blockPos.x(), blockPos.y(), blockPos.z());

            Generator generator = generatorManager.getGenerator(block);
            if(generator == null) return;

            BlockState state = block.getState();
            if (!(state instanceof TileState tile)) return;

            // истина из PDC
            Inventory tmpInv = Bukkit.createInventory(null, 27);
            MechanismStorage.loadItems(tile, tmpInv, Keys.KEY_ITEMS);
            ItemStack cellFromPdc = tmpInv.getItem(CELL_SLOT);

            playerUuids.removeIf(uuid -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null) return true; // оффлайн [web:331]

                Inventory top = p.getOpenInventory().getTopInventory(); // верхний инвентарь окна [web:239]
                if (!(top.getHolder() instanceof MechanismHolder h)) return true;

                // защита: игрок мог открыть другой генератор
                if (!BlockPosKey.of(h.getLocation()).equals(blockPos)) return true;
                int currentEnergy = generator.getEnergyLevel();
                int capacity = generator.getCapacity();
                double energyPercent = (double) (currentEnergy * 100) / capacity;
                genInventory.updateEnergyBar(top, h,energyPercent);

                // UI-обновление (при желании добавь фильтр, чтобы не перетирать игрока)
                if(!EnergyCell.isEnergyCell(cellFromPdc)) return false;
                top.setItem(CELL_SLOT, cellFromPdc);
                return false;
            });
        });
    }

    // true = оставить активным, false = убрать
    private boolean tickOne(Location loc) {
        World w = loc.getWorld();
        if (w == null) return false;

        Block block = w.getBlockAt(loc);
        Generator generator = generatorManager.getGenerator(block);
        if(generator == null) return false;

        BlockState state = block.getState();
        if (!(state instanceof TileState tile)) return false;

        Inventory inv = Bukkit.createInventory(null, 27);
        MechanismStorage.loadItems(tile, inv, Keys.KEY_ITEMS);

        ItemStack cellItem = inv.getItem(CELL_SLOT);

        if (!EnergyCell.isEnergyCell(cellItem)) return false;
        EnergyCell cell = new EnergyCell(cellItem);
        int cellEnergy = EnergyCell.getEnergy(cellItem);
        // Ячейка пуста - остаемся активными, но ничего не делаем
        if (cellEnergy <= 0) return true;

        int currentBuffer = generator.getEnergyLevel();
        int maxCapacity = generator.getCapacity();

        // Если буфер полон - ничего не делаем
        if (currentBuffer >= maxCapacity) {
            return true;
        }

        int spaceLeft = maxCapacity - currentBuffer;
        int energyToTransfer = Math.min(cell.getFrequency(), Math.min(cellEnergy, spaceLeft));

        if (energyToTransfer <= 0) {
            return true;
        }

        // ===== ПЕРЕДАЧА ЭНЕРГИИ =====
        EnergyCell.setEnergy(cellItem, cellEnergy - energyToTransfer);
        inv.setItem(CELL_SLOT, cellItem);

        // 2. Увеличиваем буфер генератора
        generator.setEnergyLevel(currentBuffer + energyToTransfer);

        // 3. Сохраняем изменения в PDC блока
        generatorManager.saveMechanism(generator);

        // 4. Обновляем PDC TileState для синхронизации
        PersistentDataContainer pdc = tile.getPersistentDataContainer();
        pdc.set(Keys.BUFFER, PersistentDataType.INTEGER, generator.getEnergyLevel());

        // 5. Сохраняем инвентарь обратно в PDC
        MechanismStorage.saveItems(tile, inv, Keys.KEY_ITEMS);

        // 6. Обновляем TileState
        tile.update();
//        EnergyCell.setEnergy(cell, cellEnergy - moved); // внутри обновится lore
//        inv.setItem(9, cell);
//
//        pdc.set(Keys.BUFFER, PersistentDataType.INTEGER, buf + moved);
//
//        MechanismStorage.saveItems(tile, inv, Keys.KEY_ITEMS); // внутри tile.update() обязательно [web:65]
        return true;
    }

}