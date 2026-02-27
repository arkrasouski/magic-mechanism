package org.example.artyom.magicMechanism.mechanisms;

import com.jeff_media.customblockdata.CustomBlockData;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.data.enums.MechanismType;
import org.example.artyom.magicMechanism.data.interfaces.IEnergyNetwork;
import org.example.artyom.magicMechanism.data.records.MechanismData;
import org.example.artyom.magicMechanism.managers.BarrierManager;
import org.example.artyom.magicMechanism.managers.CableManager;
import org.example.artyom.magicMechanism.managers.GeneratorManager;
import org.example.artyom.magicMechanism.utils.LogUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Cable extends BaseMechanism implements IEnergyNetwork {
    private MagicMechanism plugin;
    private static final int TRANSFER_RATE = 10;
    private final Set<Location> connectedCables = new HashSet<>();
    private final Set<Location> connectedMachines = new HashSet<>();
    private final Set<Location> connectedGenerators = new HashSet<>();
    private final Set<Location> connectedConsumers = new HashSet<>();
    private static final MechanismType mechanismType = MechanismType.CABLE;

    private final GeneratorManager generatorManager;
    private final BarrierManager barrierManager;
    private final int energyLevel = 0;
    private long lastScanTime = 0;
    private static final long SCAN_COOLDOWN = 1000;

    // Ленивая инициализация ключа
    private static NamespacedKey cableKey;

    private static NamespacedKey getCableKey() {
        if (cableKey == null) {
            MagicMechanism instance = MagicMechanism.getInstance();
            if (instance != null) {
                cableKey = new NamespacedKey(instance, "is_cable");
            }
        }
        return cableKey;
    }

    public Cable (Location location, UUID owner, int energyLevel, int capacity) {
        super(location, MechanismType.CABLE, owner, capacity);
        energyLevel = energyLevel;
        plugin = MagicMechanism.getInstance();
        generatorManager = plugin.getGeneratorManager();
        barrierManager = plugin.getBarrierManager();
    }

    private Cable(Location location, UUID owner) {
        super(location, MechanismType.CABLE, owner, 2);
        plugin = MagicMechanism.getInstance();
        generatorManager = plugin.getGeneratorManager();
        barrierManager = plugin.getBarrierManager();
    }

    // Фабричный метод для СОЗДАНИЯ нового кабеля (при размещении)
    public static Cable create( Location location, UUID owner, CableManager manager) {
        Cable cable = new Cable(location, owner);
        manager.addCable(location, cable);
        LogUtil.warn("✓ КАБЕЛЬ СОЗДАН: " + location);
        return cable;
    }

    // Фабричный метод для ЗАГРУЗКИ кабеля (из данных)
    public static Cable load( Location location, MechanismData data, CableManager manager) {
        Cable cable = new Cable(location, data.owner());
        // Если у кабеля будут настройки, загружаем их из data
        // Например: cable.transferRate = data.getSomething();

        manager.addCable(location, cable);
        LogUtil.warn("✓ КАБЕЛЬ ЗАГРУЖЕН: " + location);
        return cable;
    }

    // Метод для сериализации (если появятся данные)
    public MechanismData toData() {
        return new MechanismData(
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                0, // energy (у кабеля нет энергии)
                0, // maxEnergy
                true, // active
                null // owner
        );
    }

    public Location getLocation() {
        return location;
    }

    /**
     * Сканирует соседние блоки и обновляет соединения в памяти
     */
    public void scanConnections(Block block) {
        long now = System.currentTimeMillis();
        if (now - lastScanTime < SCAN_COOLDOWN) {
            LogUtil.warn("СКАНИРОВАНИЕ ПРОПУЩЕНО (cooldown)");
            return;
        }
        lastScanTime = now;

        connectedCables.clear();
        connectedMachines.clear();
        connectedGenerators.clear();
        connectedConsumers.clear();

        LogUtil.warn("========== СКАНИРОВАНИЕ КАБЕЛЯ ==========");
        LogUtil.warn("Кабель на: " + location);
        LogUtil.warn("Блок: " + block.getType());

        BlockFace[] faces = {
                BlockFace.NORTH, BlockFace.SOUTH,
                BlockFace.EAST, BlockFace.WEST,
                BlockFace.UP, BlockFace.DOWN
        };

        CableManager cableManager = plugin.getCableManager();
        GeneratorManager generatorManager = plugin.getGeneratorManager();
        BarrierManager barrierManager = plugin.getBarrierManager();

        for (BlockFace face : faces) {
            Block neighbor = block.getRelative(face);
            Location neighborLoc = neighbor.getLocation();

            LogUtil.warn("--- Проверка направления: " + face + " ---");
            LogUtil.warn("Сосед: " + neighborLoc);

            // 1. Проверяем кабели
            if (cableManager.isCable(neighbor)) {
                LogUtil.warn("✓ НАЙДЕН КАБЕЛЬ");
                connectedCables.add(neighborLoc);
                continue;
            }

            // 2. Проверяем генераторы
            Generator generator = generatorManager.getMechanism(neighborLoc);
            if (generator != null) {
                LogUtil.warn("✓ НАЙДЕН ГЕНЕРАТОР");
                connectedGenerators.add(neighborLoc);
                continue;
            }

            // 3. Проверяем барьеры - ИСПРАВЛЕНО!
            Barrier barrier = barrierManager.getMechanism(neighborLoc);
            if (barrier != null) {
                LogUtil.warn("✓ НАЙДЕН БАРЬЕР!");
                connectedMachines.add(neighborLoc);
                connectedConsumers.add(neighborLoc); // ← КРИТИЧЕСКИ ВАЖНО!
            } else {
                LogUtil.warn("✗ БАРЬЕР НЕ НАЙДЕН");
            }
        }

        LogUtil.warn("========== ИТОГИ СКАНИРОВАНИЯ ==========");
        LogUtil.warn("connectedConsumers: " + connectedConsumers.size());
    }




    // ================ IEnergyNetwork ==========================

    @Override
    public Set<Location> getConnectedConsumers() {
        return Collections.unmodifiableSet(connectedConsumers);
    }

    @Override
    public Set<Location> getConnectedGenerators() {
        return Collections.unmodifiableSet(connectedGenerators);
    }

    @Override
    public int transferEnergy(int amount, Location from) {
        if (amount <= 0) return 0;

        int remainingEnergy = amount;
        int totalTransferred = 0;

        for (Location consumerLoc : connectedConsumers) {
            if (remainingEnergy <= 0) break;

            if (barrierManager.hasMechanism(consumerLoc)) {
                Barrier barrier = barrierManager.getMechanism(consumerLoc);
                if (barrier != null && barrier.getEnergyLevel() < barrier.getCapacity()) {
                    int energyToTransfer = Math.min(TRANSFER_RATE, remainingEnergy);
                    int currentEnergy = barrier.getEnergyLevel();
                    int maxStorage = barrier.getCapacity();

                    int newEnergy = Math.min(currentEnergy + energyToTransfer, maxStorage);
                    int usedEnergy = newEnergy - currentEnergy;

                    if (usedEnergy > 0) {
                        barrier.setEnergyLevel(newEnergy);
                        barrierManager.saveMechanism(barrier);

                        remainingEnergy -= usedEnergy;
                        totalTransferred += usedEnergy;

                        LogUtil.warn(String.format("Кабель передал %d энергии барьеру %s. Теперь: %d/%d",
                                usedEnergy, consumerLoc, newEnergy, maxStorage));
                    }
                }
            }
        }

        return totalTransferred;
    }

    @Override
    public boolean isInNetwork(Location loc) {
        return connectedCables.contains(loc) ||
                connectedMachines.contains(loc) ||
                connectedGenerators.contains(loc) ||
                connectedConsumers.contains(loc);
    }

    @Override
    public Set<Location> getAllConnections() {
        Set<Location> all = new HashSet<>();
        all.addAll(connectedCables);
        all.addAll(connectedMachines);
        all.addAll(connectedGenerators);
        all.addAll(connectedConsumers);
        return Collections.unmodifiableSet(all);
    }

    // ===========================

    public Set<Location> getConnectedCables() {
        return Collections.unmodifiableSet(connectedCables);
    }

    public Set<Location> getConnectedMachines() {
        return Collections.unmodifiableSet(connectedMachines);
    }

    /**
     * Проверяет, является ли блок проводом
     */
    public boolean isCable(Block block) {
        if (block == null || block.isEmpty()) return false;

        NamespacedKey key = getCableKey();
        if (key == null) return false;

        try {
            PersistentDataContainer pdc = new CustomBlockData(block, plugin);
            LogUtil.warn(pdc.has(key, PersistentDataType.BOOLEAN) +"" +key);
            return pdc.has(key, PersistentDataType.BOOLEAN);
        } catch (Exception e) {
            LogUtil.warn("Error checking cable: " + e.getMessage());
            return false;
        }
    }

    /**
     * Статический метод для проверки (если нужен)
     */
    public static boolean isCableStatic(Block block) {
        if (block == null || block.isEmpty()) return false;

        NamespacedKey key = getCableKey();
        if (key == null) return false;

        try {
            PersistentDataContainer pdc = new CustomBlockData(block, MagicMechanism.getInstance());
            return pdc.has(key, PersistentDataType.BOOLEAN);
        } catch (Exception e) {
            return false;
        }
    }
}