package org.example.artyom.magicMechanism.managers;

import com.jeff_media.customblockdata.CustomBlockData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
    private final Map<Location, Cable> cableCache = new HashMap<>();

    // Ссылки на другие менеджеры
    private NetworkManager networkManager;
    private GeneratorManager generatorManager;
    private BarrierManager barrierManager;

    public CableManager(MagicMechanism plugin) {
        this.plugin = plugin;
        this.cableKey = new NamespacedKey(plugin, "is_cable");

        CustomBlockData.registerListener(plugin);
        LogUtil.info("CableManager инициализирован");
    }
    public void setNetworkManager(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }


    /**
     * Пометить блок как кабель и создать обработчик
     */
    public void markAsCable(Block block, Player owner) {
        Location loc = block.getLocation();

        try {
            // 1. Сохраняем в PDC
            PersistentDataContainer pdc = new CustomBlockData(block, plugin);
            pdc.set(cableKey, PersistentDataType.BOOLEAN, true);

            // 2. Создаем кабель (пока без сети)
            Cable cable = Cable.create(loc, owner.getUniqueId(), this);

            // 3. Сканируем соседей
            Set<Location> neighbors = cable.scanDirectConnections(block);

            // 4. NetworkManager сам определит и установит нужную сеть!
            if (networkManager != null) {
                networkManager.onCablePlaced(cable, loc);
            }

            LogUtil.info("✅ Кабель успешно размещен на " + loc);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Проверить, является ли блок кабелем (по PDC)
     */
    public boolean isCable(Block block) {
        if (block == null || block.isEmpty()) {
            return false;
        }

        try {
            PersistentDataContainer pdc = new CustomBlockData(block, plugin);
            return pdc.has(cableKey, PersistentDataType.BOOLEAN);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Проверить по локации (удобно для NetworkManager)
     */
    public boolean isCable(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        return isCable(loc.getBlock());
    }

    /**
     * Получить кабель из блока
     */
    public Cable getCable(Block block) {
        if (block == null) return null;
        return getCable(block.getLocation());
    }

    /**
     * Получить кабель по локации
     */
    public Cable getCable(Location loc) {
        if (loc == null) return null;

        // Сначала проверяем кэш
        Cable cable = cableCache.get(loc);
        if (cable != null) {
            return cable;
        }

        // Если нет в кэше, проверяем PDC
        if (isCable(loc.getBlock())) {
            // Загружаем из конфига или создаем с null owner
            MechanismData data = loadCableData(loc);
            cable = Cable.load(loc, data != null ? data :
                    new MechanismData(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                            0, 0, true, null), this);



            // Кэшируем
            cableCache.put(loc, cable);

            // Уведомляем NetworkManager о загруженном кабеле
            if (networkManager != null) {
                networkManager.onCablePlaced(cable, loc);
            }

            return cable;
        }

        return null;
    }

    /**
     * Добавить кабель в менеджер (вызывается из фабрики)
     */
    public void addCable(Location location, Cable cable) {
        cableCache.put(location, cable);
        saveCable(cable);
        if (networkManager != null) {
            LogUtil.warn("📢 Уведомляем NetworkManager о новом кабеле " + location);
            networkManager.onCablePlaced(cable, location);
        }
        LogUtil.warn("Кабель добавлен в менеджер: " + location);
    }

    /**
     * Удалить кабель
     */
    public void removeCable(Block block, Player owner) {
        Location loc = block.getLocation();
        Cable cable = cableCache.get(loc);

        // 1. Уведомляем NetworkManager ДО удаления
        if (networkManager != null && cable != null) {
            networkManager.onCableRemoved(loc);
        }

        // 2. Удаляем из PDC
        PersistentDataContainer pdc = new CustomBlockData(block, plugin);
        pdc.remove(cableKey);

        // 3. Удаляем из кэша
        cableCache.remove(loc);

        // 4. Удаляем из конфига
        removeCableFromConfig(loc);

        // 5. Обновляем соседние кабели (они должны пересканировать соседей)
        updateNeighborsAfterRemoval(block);

        LogUtil.info("Кабель удален с " + loc);
    }

    /**
     * Обновить соседей после удаления кабеля
     */
    private void updateNeighborsAfterRemoval(Block block) {
        BlockFace[] faces = {
                BlockFace.NORTH, BlockFace.SOUTH,
                BlockFace.EAST, BlockFace.WEST,
                BlockFace.UP, BlockFace.DOWN
        };

        for (BlockFace face : faces) {
            Block neighbor = block.getRelative(face);
            Cable neighborCable = getCable(neighbor);
            if (neighborCable != null) {
                // Пересканируем соседей
                Set<Location> newNeighbors = neighborCable.scanDirectConnections(neighbor);

                // Уведомляем NetworkManager об изменении
                if (networkManager != null) {
                    networkManager.onCableNeighborsChanged(neighbor.getLocation(), newNeighbors);
                }
            }
        }
    }

    // ========== РАБОТА С КОНФИГОМ ==========

    /**
     * Сохранить кабель в конфиг
     */
    public void saveCable(Cable cable) {
        MechanismData data = cable.toData();
        String path = getConfigPath(cable.getLocation());

        plugin.getConfig().set(path + ".x", data.x());
        plugin.getConfig().set(path + ".y", data.y());
        plugin.getConfig().set(path + ".z", data.z());
        plugin.getConfig().set(path + ".active", data.active());
        if (data.owner() != null) {
            plugin.getConfig().set(path + ".owner", data.owner().toString());
        }

        plugin.saveConfig();
    }

    /**
     * Удалить кабель из конфига
     */
    private void removeCableFromConfig(Location loc) {
        String path = getConfigPath(loc);
        plugin.getConfig().set(path, null);
        plugin.saveConfig();
    }

    /**
     * Загрузить данные кабеля из конфига
     */
    private MechanismData loadCableData(Location loc) {
        String path = getConfigPath(loc);

        if (!plugin.getConfig().contains(path)) {
            return null;
        }

        try {
            int x = plugin.getConfig().getInt(path + ".x");
            int y = plugin.getConfig().getInt(path + ".y");
            int z = plugin.getConfig().getInt(path + ".z");
            int energy = plugin.getConfig().getInt(path + ".energy", 0);
            int maxEnergy = plugin.getConfig().getInt(path + ".maxEnergy", 0);
            boolean active = plugin.getConfig().getBoolean(path + ".active", true);

            UUID owner = null;
            if (plugin.getConfig().contains(path + ".owner")) {
                owner = UUID.fromString(plugin.getConfig().getString(path + ".owner"));
            }

            return new MechanismData(x, y, z, energy, maxEnergy, active, owner);

        } catch (Exception e) {
            LogUtil.warn("Ошибка загрузки данных кабеля: " + e.getMessage());
            return null;
        }
    }

    private String getConfigPath(Location loc) {
        return "cables." + loc.getWorld().getName() + "_" +
                loc.getBlockX() + "_" +
                loc.getBlockY() + "_" +
                loc.getBlockZ();
    }

    // ========== ЗАГРУЗКА/ВЫГРУЗКА ЧАНКОВ ==========

    /**
     * Загрузить кабели в чанке
     */
    public void loadCablesInChunk(Chunk chunk) {
        World world = chunk.getWorld();

        LogUtil.warn("=== ЗАГРУЗКА КАБЕЛЕЙ В ЧАНКЕ " + chunk.getX() + "," + chunk.getZ() + " ===");

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                    Block block = chunk.getBlock(x, y, z);

                    if (!isCable(block)) {
                        continue;
                    }

                    Location loc = block.getLocation();

                    // Пропускаем, если уже в кэше
                    if (cableCache.containsKey(loc)) {
                        continue;
                    }

                    // Загружаем данные
                    MechanismData data = loadCableData(loc);

                    // Создаем кабель
                    Cable cable = Cable.load(loc, data != null ? data :
                            new MechanismData(x, y, z, 0, 0, true, null), this);

                    // Сканируем соседей
                    Set<Location> neighbors = cable.scanDirectConnections(block);

                    // Кэшируем
                    cableCache.put(loc, cable);

                    // Уведомляем NetworkManager
                    if (networkManager != null) {
                        networkManager.onCablePlaced(cable, loc);
                    }

                    LogUtil.warn("  ✓ Загружен кабель: " + loc);
                }
            }
        }

        LogUtil.warn("=== ИТОГ ЗАГРУЗКИ ЧАНКА ===");
        LogUtil.warn("Всего кабелей в кэше: " + cableCache.size());
    }

    /**
     * Выгрузить кабели в чанке
     */
    public void unloadCablesInChunk(Chunk chunk) {
        // Находим все кабели в этом чанке
        List<Location> toRemove = new ArrayList<>();

        for (Location loc : cableCache.keySet()) {
            if (loc.getChunk().equals(chunk)) {
                toRemove.add(loc);
            }
        }

        // Уведомляем NetworkManager перед удалением
        if (networkManager != null) {
            for (Location loc : toRemove) {
                networkManager.onCableRemoved(loc);
            }
        }

        // Удаляем из кэша
        toRemove.forEach(cableCache::remove);

        LogUtil.warn("Выгружено " + toRemove.size() + " кабелей в чанке " +
                chunk.getX() + "," + chunk.getZ());
    }

    /**
     * Загрузить все кабели из загруженных чанков
     */
    public void loadAllCables() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                loadCablesInChunk(chunk);
            }
        }
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    /**
     * Получить соседей для локации (используется NetworkManager)
     */
    public Set<Location> getNeighbors(Location loc) {
        Set<Location> neighbors = new HashSet<>();
        Block block = loc.getBlock();

        BlockFace[] faces = {
                BlockFace.NORTH, BlockFace.SOUTH,
                BlockFace.EAST, BlockFace.WEST,
                BlockFace.UP, BlockFace.DOWN
        };

        for (BlockFace face : faces) {
            Block neighbor = block.getRelative(face);
            Location neighborLoc = neighbor.getLocation();

            // Проверяем все типы механизмов
            if (isCable(neighbor) ||
                    (generatorManager != null && generatorManager.hasMechanism(neighborLoc)) ||
                    (barrierManager != null && barrierManager.hasMechanism(neighborLoc))) {
                neighbors.add(neighborLoc);
            }
        }

        return neighbors;
    }

    /**
     * Получить все кабели
     */
    public Collection<Cable> getAllCables() {
        return Collections.unmodifiableCollection(cableCache.values());
    }

    /**
     * Очистить кэш
     */
    public void clearCache() {
        cableCache.clear();
        LogUtil.info("Кэш кабелей очищен");
    }

    /**
     * Обновить все кабели (периодический тик)
     */
    public void tick() {
        // Ничего не делаем - вся логика в NetworkManager
        // Кабели только хранят данные о соседях
    }
}