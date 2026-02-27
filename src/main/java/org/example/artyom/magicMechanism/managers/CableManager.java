package org.example.artyom.magicMechanism.managers;

import com.jeff_media.customblockdata.CustomBlockData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.data.records.MechanismData;
import org.example.artyom.magicMechanism.mechanisms.Cable;
import org.example.artyom.magicMechanism.utils.LogUtil;

import java.util.*;

public class CableManager {

    private final MagicMechanism plugin;
    private final NamespacedKey cableKey;
    private final Map<Location, Cable> cableCache = new HashMap<>(); // Кэш для быстрого доступа

    public CableManager(MagicMechanism plugin) {
        this.plugin = plugin;
        this.cableKey = new NamespacedKey(plugin, "is_cable");

        // Регистрируем слушатель для CustomBlockData
        CustomBlockData.registerListener(plugin);
        LogUtil.info("CableManager инициализирован");
    }

    /**
     * Пометить блок как кабель и создать обработчик
     */
    public void markAsCable(Block block, Player owner) {
        Location loc = block.getLocation();

        LogUtil.warn("=== НАЧАЛО markAsCable ===");
        LogUtil.warn("Блок: " + loc);
        LogUtil.warn("Материал: " + block.getType());

        try {
            // 1. Сохраняем в PDC
            PersistentDataContainer pdc = new CustomBlockData(block, plugin);

            // Проверяем, не был ли блок уже кабелем
            if (pdc.has(cableKey, PersistentDataType.BOOLEAN)) {
                LogUtil.warn("Блок уже был кабелем! Обновляем...");
            }

            // Устанавливаем метку
            pdc.set(cableKey, PersistentDataType.BOOLEAN, true);
            LogUtil.warn("✓ Метка кабеля установлена в PDC");

            // Проверяем, что метка сохранилась
            boolean saved = pdc.has(cableKey, PersistentDataType.BOOLEAN);
            if (!saved) {
                LogUtil.warn("⚠ ОШИБКА: Метка не сохранилась в PDC!");
                return;
            }

            LogUtil.warn("✓ Метка подтверждена в PDC");

            // 2. ИСПОЛЬЗУЕМ ФАБРИЧНЫЙ МЕТОД для создания кабеля
            Cable cable = Cable.create(loc, owner.getUniqueId(), this);
            LogUtil.warn("✓ Кабель создан через фабрику");

            // 3. Сканируем соединения
            cable.scanConnections(block);
            LogUtil.warn("✓ Соединения отсканированы");

            // 4. Обновляем соседние кабели
            updateNeighborCables(block, owner);
            LogUtil.warn("✓ Соседние кабели обновлены");

            LogUtil.info("✅ Кабель успешно размещен на " + loc);

        } catch (Exception e) {
            LogUtil.warn("❌ ОШИБКА в markAsCable: " + e.getMessage());
            e.printStackTrace();
        }

        LogUtil.warn("=== КОНЕЦ markAsCable ===");
    }
    public boolean isCable(Block block) {
        if (block == null || block.isEmpty()) {
            return false;
        }

        try {
            PersistentDataContainer pdc = new CustomBlockData(block, plugin);

            // ВАЖНО: Используем BOOLEAN, а не STRING!
            boolean hasKey = pdc.has(cableKey, PersistentDataType.BOOLEAN);

            if (hasKey) {
                // Читаем как BOOLEAN
                Boolean value = pdc.get(cableKey, PersistentDataType.BOOLEAN);
                LogUtil.warn("Кабель найден в PDC, значение: " + value);
                return value;
            }

            return false;

        } catch (Exception e) {
            LogUtil.warn("Ошибка в isCable: " + e.getMessage());
            return false;
        }
    }
    /**
     * Получить кабель из блока (с проверкой кэша)
     */
    public Cable getCable(Block block) {
        if (block == null) return null;
        Location loc = block.getLocation();

        // Сначала проверяем кэш
        if (cableCache.containsKey(loc)) {
            LogUtil.warn("Кабель найден в кэше: " + loc);
            return cableCache.get(loc);
        }

        // Если нет в кэше, проверяем PDC напрямую
        if (isCable(block)) {  // Используем прямой метод проверки
            LogUtil.warn("Кабель найден в PDC, создаем новый: " + loc);
            Cable cable = Cable.create(loc, null, this);
            LogUtil.warn("✓ Кабель создан через фабрику");
            cableCache.put(loc, cable);
            cable.scanConnections(block);
            return cable;
        }



        return null;
    }
    // Добавление кабеля (вызывается из фабрики)
    public void addCable(Location location, Cable cable) {
        cableCache.put(location, cable);
        saveCable(cable);
        LogUtil.warn("Кабель добавлен в менеджер: " + location);
    }
    /**
     * Проверить, является ли блок кабелем (по PDC)
     */

    // Сохранение кабеля
    public void saveCable(Cable cable) {
        MechanismData data = cable.toData();
        String path = "cables." + locationToString(cable.getLocation());

        // Сохраняем данные кабеля
        plugin.getConfig().set(path + ".x", data.x());
        plugin.getConfig().set(path + ".y", data.y());
        plugin.getConfig().set(path + ".z", data.z());
        plugin.getConfig().set(path + ".active", data.active());

        plugin.saveConfig();
    }

    private String locationToString(Location loc) {
        return loc.getWorld().getName() + "_" +
                loc.getBlockX() + "_" +
                loc.getBlockY() + "_" +
                loc.getBlockZ();
    }
    /**
     * Удалить кабель
     */
    public void removeCable(Block block, Player owner) {
        Location loc = block.getLocation();

        // Удаляем из кэша и EnergyManager
        cableCache.remove(loc);
        EnergyManager.removeHandler(loc);

        // Удаляем из PDC
        PersistentDataContainer pdc = new CustomBlockData(block, plugin);
        pdc.remove(cableKey);

        // Обновляем соседние кабели
        updateNeighborCables(block, owner);
        LogUtil.info("Кабель удален с " + loc);
    }

    /**
     * Загрузить все кабели из загруженных чанков
     */
    private void loadAllCables() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                loadCablesInChunk(chunk);
            }
        }
    }

    /**
     * Загрузить кабели в конкретном чанке
     */
    public void loadCablesInChunk(Chunk chunk) {
        World world = chunk.getWorld();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        LogUtil.warn("=== ЗАГРУЗКА КАБЕЛЕЙ В ЧАНКЕ " + chunkX + "," + chunkZ + " ===");

        // Загружаем из конфига данные для этого чанка
        Map<Location, MechanismData> cableData = loadCableDataFromConfig(chunk);

        // Проходим по всем блокам в чанке
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                    Block block = chunk.getBlock(x, y, z);
                    Location loc = block.getLocation();

                    // Быстрая проверка материала
                    if (!isCable(block)) {
                        continue;
                    }

                    // Проверяем PDC
                    if (isCable(block)) {
                        // Пропускаем, если уже в кэше
                        if (cableCache.containsKey(loc)) {
                            continue;
                        }

                        // Получаем данные из конфига или создаем с null owner
                        MechanismData data = cableData.get(loc);
                        UUID owner = (data != null) ? data.owner() : null;

                        // СОЗДАЕМ КАБЕЛЬ ЧЕРЕЗ ФАБРИКУ С OWNER
                        Cable cable = Cable.load(loc, data != null ? data :
                                new MechanismData(x, y, z, 0, 0, true, null), this);

                        // Сканируем соединения
                        cable.scanConnections(block);

                        LogUtil.warn("  ✓ Загружен кабель: " + loc + " владелец: " + owner);
                    }
                }
            }
        }

        LogUtil.warn("=== ИТОГ ЗАГРУЗКИ ЧАНКА ===");
        LogUtil.warn("Всего кабелей в кэше: " + cableCache.size());
    }

    /**
     * Загружает данные кабелей для чанка из конфига
     */
    private Map<Location, MechanismData> loadCableDataFromConfig(Chunk chunk) {
        Map<Location, MechanismData> result = new HashMap<>();
        String chunkKey = chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ();

        if (!plugin.getConfig().contains("cables." + chunkKey)) {
            return result;
        }

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("cables." + chunkKey);
        for (String key : section.getKeys(false)) {
            try {
                int x = section.getInt(key + ".x");
                int y = section.getInt(key + ".y");
                int z = section.getInt(key + ".z");
                int energy = section.getInt(key + ".energy", 0);
                int maxEnergy = section.getInt(key + ".maxEnergy", 0);
                boolean active = section.getBoolean(key + ".active", true);

                UUID owner = null;
                if (section.contains(key + ".owner")) {
                    owner = UUID.fromString(section.getString(key + ".owner"));
                }

                Location loc = new Location(chunk.getWorld(), x, y, z);
                result.put(loc, new MechanismData(x, y, z, energy, maxEnergy, active, owner));

            } catch (Exception e) {
                LogUtil.warn("Ошибка загрузки данных кабеля: " + e.getMessage());
            }
        }

        return result;
    }

    /**
     * Выгрузить кабели в чанке (при выгрузке чанка)
     */
    public void unloadCablesInChunk(Chunk chunk) {
        // Удаляем из кэша все кабели в этом чанке
        cableCache.entrySet().removeIf(entry ->
                entry.getKey().getChunk().equals(chunk)
        );
    }

    /**
     * Обновить соседние кабели
     */
    public void updateNeighborCables(Block block, Player owner) {
        BlockFace[] faces = {
                BlockFace.NORTH, BlockFace.SOUTH,
                BlockFace.EAST, BlockFace.WEST,
                BlockFace.UP, BlockFace.DOWN
        };

        for (BlockFace face : faces) {
            Block neighbor = block.getRelative(face);
            Cable neighborCable = getCable(neighbor);
            if (neighborCable != null) {
                neighborCable.scanConnections(neighbor);
            }
        }
    }


    /**
     * Найти сеть для блока (рекурсивно)
     */
    public Set<Cable> findNetwork(Block startBlock, Player owner) {
        Set<Cable> network = new HashSet<>();
        Set<Location> visited = new HashSet<>();

        findNetworkRecursive(startBlock, owner, network, visited);

        return network;
    }

    private void findNetworkRecursive(Block block, Player owner, Set<Cable> network, Set<Location> visited) {
        if (block == null) return;

        Location loc = block.getLocation();
        if (visited.contains(loc)) return;
        visited.add(loc);

        Cable cable = getCable(block);
        if (cable != null) {
            network.add(cable);

            // Рекурсивно проверяем все подключенные кабели
            for (Location cableLoc : cable.getConnectedCables()) {
                Block cableBlock = cableLoc.getBlock();
                findNetworkRecursive(cableBlock, owner, network, visited);
            }
        }
    }

    /**
     * Получить все кабели
     */
    public Collection<Cable> getAllCables() {
        return Collections.unmodifiableCollection(cableCache.values());
    }

    /**
     * Очистить кэш (при выключении)
     */
    public void clearCache() {
        cableCache.clear();
        LogUtil.info("Кэш кабелей очищен");
    }
}
