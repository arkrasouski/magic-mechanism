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
import org.example.artyom.magicMechanism.managers.NetworkManager;
import org.example.artyom.magicMechanism.network.EnergyNetwork;
import org.example.artyom.magicMechanism.utils.LogUtil;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.example.artyom.magicMechanism.utils.BlockUtil.FACES;

public class Cable extends BaseMechanism {
    private MagicMechanism plugin;
    private final Set<Location> directConnections = new HashSet<>(); // только прямые соседи
    private NetworkManager networkManager;
    private BarrierManager barrierManager;
    private GeneratorManager generatorManager;
    private CableManager cableManager;
    // Ленивая инициализация ключа
    private EnergyNetwork network;
    private static NamespacedKey cableKey;
    private long lastScanTime = 0;
    private static final long SCAN_COOLDOWN = 1000;

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
        plugin = MagicMechanism.getInstance();
        this.generatorManager = plugin.getGeneratorManager();
        this.barrierManager = plugin.getBarrierManager();
        this.networkManager = plugin.getNetworkManager();
        this.cableManager = plugin.getCableManager();
    }

    private Cable(Location location, UUID owner) {
        super(location, MechanismType.CABLE, owner, 2);
        plugin = MagicMechanism.getInstance();
        this.generatorManager = plugin.getGeneratorManager();
        this.barrierManager = plugin.getBarrierManager();
        this.networkManager = plugin.getNetworkManager();
        this.cableManager = plugin.getCableManager();

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

    public Set<Location> scanDirectConnections(Block block) {
        
            long now = System.currentTimeMillis();
            if (now - lastScanTime < SCAN_COOLDOWN) {
                LogUtil.warn("🕐 СКАНИРОВАНИЕ ПРОПУЩЕНО (cooldown) - используем кэш: " + directConnections.size() + " соседей");
                return new HashSet<>(directConnections);
            }
            lastScanTime = now;

            Set<Location> olddirectConnections = new HashSet<>(directConnections);
            directConnections.clear();

            LogUtil.warn("========== 🔍 СКАНИРОВАНИЕ КАБЕЛЯ ==========");
            LogUtil.warn("📍 Кабель на: " + formatLocation(location));
            LogUtil.warn("🔲 Блок: " + block.getType());

            BlockFace[] faces = {
                    BlockFace.NORTH, BlockFace.SOUTH,
                    BlockFace.EAST, BlockFace.WEST,
                    BlockFace.UP, BlockFace.DOWN
            };

            MagicMechanism plugin = MagicMechanism.getInstance();
            CableManager cableManager = plugin.getCableManager();
            GeneratorManager generatorManager = plugin.getGeneratorManager();
            BarrierManager barrierManager = plugin.getBarrierManager();

            LogUtil.warn("📋 Проверяем 6 направлений:");

            for (BlockFace face : faces) {
                Block neighbor = block.getRelative(face);
                Location neighborLoc = neighbor.getLocation();

                LogUtil.warn("  → Направление " + face + ": " + formatLocation(neighborLoc));

                // 1. Проверяем кабели
                if (cableManager != null && cableManager.isCable(neighbor)) {
                    LogUtil.warn("    ✅ НАЙДЕН КАБЕЛЬ!");
                    directConnections.add(neighborLoc);
                    continue;
                }

                // 2. Проверяем генераторы
                if (generatorManager != null) {
                    Generator generator = generatorManager.getMechanism(neighborLoc);
                    if (generator != null) {
                        LogUtil.warn("    ✅ НАЙДЕН ГЕНЕРАТОР! Энергия: " + generator.getEnergyLevel() + "/" + generator.getCapacity());
                        directConnections.add(neighborLoc);
                        continue;
                    }
                }

                // 3. Проверяем барьеры
                if (barrierManager != null) {
                    Barrier barrier = barrierManager.getMechanism(neighborLoc);
                    if (barrier != null) {
                        LogUtil.warn("    ✅ НАЙДЕН БАРЬЕР! Энергия: " + barrier.getEnergyLevel() + "/" + barrier.getCapacity());
                        directConnections.add(neighborLoc);
                        continue;
                    }
                }

                LogUtil.warn("    ❌ НИЧЕГО НЕ НАЙДЕНО");
            }

            LogUtil.warn("========== 📊 ИТОГИ СКАНИРОВАНИЯ ==========");
            LogUtil.warn("  Всего соседей: " + directConnections.size());
            for (Location loc : directConnections) {
                String type = getNeighborType(loc);
                LogUtil.warn("    • " + type + " на " + formatLocation(loc));
            }

            // Если соседей нет, выводим возможные причины
            if (directConnections.isEmpty()) {
                LogUtil.warn("⚠ ВНИМАНИЕ: Соседей не найдено! Возможные причины:");
                LogUtil.warn("  - Генератор/барьер не зарегистрированы в менеджерах");
                LogUtil.warn("  - Блоки находятся не рядом (проверь координаты)");
                LogUtil.warn("  - Проблема с PDC метками");
            }

            // Проверяем, изменились ли соседи
            if (!olddirectConnections.equals(directConnections)) {
                LogUtil.warn("🔄 СОСТАВ СОСЕДЕЙ ИЗМЕНИЛСЯ!");
                LogUtil.warn("  Было: " + olddirectConnections.size() + " соседей");
                LogUtil.warn("  Стало: " + directConnections.size() + " соседей");
            }

            LogUtil.warn("=================================");

            return new HashSet<>(directConnections);
        }

        private String formatLocation(Location loc) {
            return String.format("[%d %d %d]", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        }

        private String getNeighborType(Location loc) {
            MagicMechanism plugin = MagicMechanism.getInstance();
            if (plugin.getCableManager().isCable(loc.getBlock())) return "КАБЕЛЬ";
            if (plugin.getGeneratorManager().hasMechanism(loc)) return "ГЕНЕРАТОР";
            if (plugin.getBarrierManager().hasMechanism(loc)) return "БАРЬЕР";
            return "НЕИЗВЕСТНО";
        }

    public void setNetwork(EnergyNetwork network) {
        this.network = network;
    }

    public EnergyNetwork getNetwork() {
        return network;
    }

    public Set<Location> getDirectConnections() {
        return Collections.unmodifiableSet(directConnections);
    }

}