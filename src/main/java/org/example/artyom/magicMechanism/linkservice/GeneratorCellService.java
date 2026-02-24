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
import org.example.artyom.magicMechanism.data.GeneratorGuiManager;
import org.example.artyom.magicMechanism.data.Keys;
import org.example.artyom.magicMechanism.data.records.BlockPosKey;
import org.example.artyom.magicMechanism.energyitems.EnergyCell;
import org.example.artyom.magicMechanism.inventories.MechanismHolder;
import org.example.artyom.magicMechanism.inventories.MechanismStorage;
import org.example.artyom.magicMechanism.inventories.generator.GenInventory;
import org.example.artyom.magicMechanism.managers.GeneratorManager;
import org.example.artyom.magicMechanism.mechanisms.Generator;
import org.example.artyom.magicMechanism.utils.LogUtil;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class GeneratorCellService {
    private final GeneratorGuiManager guiManager;
    private final GeneratorManager generatorManager;
    private final GenInventory genInventory;
    private static final int UPDATE_INTERVAL = 20; // обновлять GUI каждые 20 тиков (1 сек)
    private static final int CELL_SLOT = 9;
    private int guiUpdateCounter = 0;

    // Счетчик для периодического сохранения
    private int saveCounter = 0;
    private static final int SAVE_INTERVAL = 100; // Сохраняем каждые 100 тиков (5 секунд)

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

    // ========== Основной тикер ==========
    public void tickAll() {
        boolean anyChanges = false;

        // Обрабатываем активные генераторы
        for (Location loc : ACTIVE) {
            if (tickOne(loc)) {
                anyChanges = true;
            }
        }

        // Удаляем неактивные
        ACTIVE.removeIf(loc -> !isGeneratorValid(loc));

        // Обновляем GUI
        guiUpdateCounter++;
        if (guiUpdateCounter >= UPDATE_INTERVAL) {
            tickOpenGuis();
            guiUpdateCounter = 0;
        }

        // Сохраняем изменения периодически
        saveCounter++;
        if (saveCounter >= SAVE_INTERVAL) {
            if (anyChanges) {
                LogUtil.warn("Автосохранение генераторов после передачи энергии");
                // Сохраняем все активные генераторы
                for (Location loc : ACTIVE) {
                    Generator gen = generatorManager.getMechanism(loc);
                    if (gen != null) {
                        generatorManager.saveMechanism(gen);
                    }
                }
            }
            saveCounter = 0;
        }
    }

    private boolean isGeneratorValid(Location loc) {
        World w = loc.getWorld();
        if (w == null) return false;

        Block block = w.getBlockAt(loc);
        Generator generator = generatorManager.getMechanism(block);
        if (generator == null) return false;

        BlockState state = block.getState();
        if (!(state instanceof TileState)) return false;

        return true;
    }

    public void tickOpenGuis() {
        guiManager.viewers().forEach((blockPos, playerUuids) -> {
            World w = Bukkit.getWorld(blockPos.worldId());
            if (w == null) return;

            Block block = w.getBlockAt(blockPos.x(), blockPos.y(), blockPos.z());

            Generator generator = generatorManager.getMechanism(block);
            if(generator == null) return;

            BlockState state = block.getState();
            if (!(state instanceof TileState tile)) return;

            // истина из PDC
            Inventory tmpInv = Bukkit.createInventory(null, 27);
            MechanismStorage.loadItems(tile, tmpInv, Keys.KEY_ITEMS);
            ItemStack cellFromPdc = tmpInv.getItem(CELL_SLOT);

            playerUuids.removeIf(uuid -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null) return true; // оффлайн

                Inventory top = p.getOpenInventory().getTopInventory();
                if (!(top.getHolder() instanceof MechanismHolder h)) return true;

                // защита: игрок мог открыть другой генератор
                if (!BlockPosKey.of(h.getLocation()).equals(blockPos)) return true;

                int currentEnergy = generator.getEnergyLevel();
                int capacity = generator.getCapacity();
                double energyPercent = capacity > 0 ? (double) (currentEnergy * 100) / capacity : 0;

                genInventory.updateEnergyBar(top, h, energyPercent);

                // UI-обновление
                if(EnergyCell.isEnergyCell(cellFromPdc)) {
                    top.setItem(CELL_SLOT, cellFromPdc);
                }
                return false;
            });
        });
    }

    /**
     * Обрабатывает один генератор
     * @param loc позиция генератора
     * @return true если были изменения энергии
     */
    private boolean tickOne(Location loc) {
        World w = loc.getWorld();
        if (w == null) return false;

        Block block = w.getBlockAt(loc);
        Generator generator = generatorManager.getMechanism(block);
        if(generator == null) return false;

        BlockState state = block.getState();
        if (!(state instanceof TileState tile)) return false;

        Inventory inv = Bukkit.createInventory(null, 27);
        MechanismStorage.loadItems(tile, inv, Keys.KEY_ITEMS);

        ItemStack cellItem = inv.getItem(CELL_SLOT);

        if (!EnergyCell.isEnergyCell(cellItem)) return false;

        int cellEnergy = EnergyCell.getEnergy(cellItem);
        // Ячейка пуста - остаемся активными, но ничего не делаем
        if (cellEnergy <= 0) return false;

        int currentBuffer = generator.getEnergyLevel();
        int maxCapacity = generator.getCapacity();

        // Если буфер полон - ничего не делаем
        if (currentBuffer >= maxCapacity) {
            return false;
        }

        int spaceLeft = maxCapacity - currentBuffer;
        EnergyCell cell = new EnergyCell(cellItem);
        int energyToTransfer = Math.min(cell.getFrequency(), Math.min(cellEnergy, spaceLeft));

        if (energyToTransfer <= 0) {
            return false;
        }

        // ===== ПЕРЕДАЧА ЭНЕРГИИ =====

        // 1. Уменьшаем энергию в ячейке
        EnergyCell.setEnergy(cellItem, cellEnergy - energyToTransfer);
        inv.setItem(CELL_SLOT, cellItem);

        // 2. Увеличиваем буфер генератора
        generator.setEnergyLevel(currentBuffer + energyToTransfer);

        // 3. Обновляем PDC TileState
        PersistentDataContainer pdc = tile.getPersistentDataContainer();
        pdc.set(Keys.BUFFER, PersistentDataType.INTEGER, generator.getEnergyLevel());

        // 4. Сохраняем инвентарь в PDC
        MechanismStorage.saveItems(tile, inv, Keys.KEY_ITEMS);

        // 5. Обновляем TileState
        tile.update();

        // НЕ СОХРАНЯЕМ генератор здесь! Это будет делать saveCounter

        LogUtil.warn(String.format("Передано %d энергии от ячейки к генератору. Теперь: %d/%d",
                energyToTransfer, generator.getEnergyLevel(), maxCapacity));

        return true; // были изменения
    }
}