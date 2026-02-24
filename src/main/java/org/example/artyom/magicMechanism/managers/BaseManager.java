package org.example.artyom.magicMechanism.managers;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.data.enums.MechanismType;
import org.example.artyom.magicMechanism.data.records.ChunkCoordinate;
import org.example.artyom.magicMechanism.data.records.MechanismData;
import org.example.artyom.magicMechanism.mechanisms.BaseMechanism;
import org.example.artyom.magicMechanism.utils.LogUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseManager<T extends BaseMechanism> {
    protected final MagicMechanism plugin;
    protected final MechanismType mechanismType;

    // Ключи для хранения в PDC чанка
    protected final NamespacedKey mechanismCountKey;
    protected final NamespacedKey mechanismListKey;

    // Основные кэши
    protected final Map<Location, T> activeMechanisms = new ConcurrentHashMap<>();
    protected final Map<ChunkCoordinate, Set<Location>> mechanismsByChunk = new ConcurrentHashMap<>();

    public BaseManager(MagicMechanism plugin, MechanismType mechanismType) {
        this.plugin = plugin;
        this.mechanismType = mechanismType;

        // Инициализация ключей для хранения списка механизмов в чанке
        String typeName = mechanismType.name().toLowerCase();
        this.mechanismCountKey = new NamespacedKey(plugin, typeName + "_count");
        this.mechanismListKey = new NamespacedKey(plugin, typeName + "_list");
    }

    // ========== АБСТРАКТНЫЕ МЕТОДЫ ==========

    /**
     * Создает новый экземпляр механизма
     */
    protected abstract T createMechanismInstance(Location location, Player owner,
                                                 int energy, int capacity, boolean active);

    /**
     * Создает данные механизма для сериализации
     */
    protected MechanismData createMechanismData(T mechanism) {
        return new MechanismData(
                mechanism.getLocation().getBlockX(),
                mechanism.getLocation().getBlockY(),
                mechanism.getLocation().getBlockZ(),
                mechanism.getEnergyLevel(),
                mechanism.getCapacity(),
                mechanism.isActive(),
                mechanism.getOwner() != null ? mechanism.getOwner().getUniqueId() : null
        );
    }

    /**
     * Десериализует механизм из данных
     */
    protected abstract T deserializeMechanism(MechanismData data, World world);

    // ========== РАБОТА С PDC ЧАНКА ==========

    /**
     * Загружает все механизмы из чанка
     */
    public void loadMechanismsFromChunk(Chunk chunk) {
        ChunkCoordinate chunkCoord = ChunkCoordinate.of(chunk);
        PersistentDataContainer chunkData = chunk.getPersistentDataContainer();

        // Получаем количество механизмов в чанке
        int mechanismCount = chunkData.getOrDefault(mechanismCountKey, PersistentDataType.INTEGER, 0);
        if (mechanismCount == 0) return;

        // Получаем список механизмов в виде строки
        String mechanismsString = chunkData.get(mechanismListKey, PersistentDataType.STRING);
        if (mechanismsString == null || mechanismsString.isEmpty()) return;

        // Разбираем строку на отдельные записи
        String[] mechanismEntries = mechanismsString.split("\\|");
        Set<Location> chunkMechanisms = new HashSet<>();

        for (String entry : mechanismEntries) {
            if (entry.isEmpty()) continue;

            MechanismData data = MechanismData.deserialize(entry);
            if (data == null) continue;

            T mechanism = deserializeMechanism(data, chunk.getWorld());
            if (mechanism == null) continue;

            Location loc = mechanism.getLocation();

            // Добавляем в кэш
            activeMechanisms.put(loc, mechanism);
            chunkMechanisms.add(loc);

            LogUtil.warn("Загружен " + mechanismType.getGuiTitle() +
                    " из чанка: " + loc);
        }

        // Сохраняем индекс
        mechanismsByChunk.put(chunkCoord, chunkMechanisms);

        LogUtil.info("Загружено " + chunkMechanisms.size() + " " +
                mechanismType.getGuiTitle() + " из чанка " +
                chunk.getX() + ", " + chunk.getZ());
    }

    /**
     * Сохраняет все механизмы указанного чанка
     */
    public void saveMechanismsFromChunk(Chunk chunk) {
        ChunkCoordinate chunkCoord = ChunkCoordinate.of(chunk);
        Set<Location> chunkMechanisms = mechanismsByChunk.get(chunkCoord);

        if (chunkMechanisms == null || chunkMechanisms.isEmpty()) {
            clearChunkMechanismData(chunk);
            return;
        }

        PersistentDataContainer chunkData = chunk.getPersistentDataContainer();

        // Сериализуем все механизмы чанка
        List<String> serializedMechanisms = new ArrayList<>();

        for (Location loc : chunkMechanisms) {
            T mechanism = activeMechanisms.get(loc);
            if (mechanism == null) continue;

            MechanismData data = createMechanismData(mechanism);
            serializedMechanisms.add(data.serialize());
        }

        // Сохраняем в PDC чанка
        String mechanismsString = String.join("|", serializedMechanisms);
        chunkData.set(mechanismListKey, PersistentDataType.STRING, mechanismsString);
        chunkData.set(mechanismCountKey, PersistentDataType.INTEGER, serializedMechanisms.size());

        LogUtil.warn("Сохранено " + serializedMechanisms.size() + " " +
                mechanismType.getGuiTitle() + " в чанк " +
                chunk.getX() + ", " + chunk.getZ());
    }

    /**
     * Очищает данные о механизмах в чанке
     */
    private void clearChunkMechanismData(Chunk chunk) {
        PersistentDataContainer chunkData = chunk.getPersistentDataContainer();
        chunkData.remove(mechanismCountKey);
        chunkData.remove(mechanismListKey);
    }

    /**
     * Загружает все механизмы из всех загруженных чанков
     */
    public void loadAllMechanismsFromLoadedChunks() {
        for (World world : plugin.getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                loadMechanismsFromChunk(chunk);
            }
        }

        LogUtil.info("Загружено всего " + activeMechanisms.size() + " " +
                mechanismType.getGuiTitle());
    }

    // ========== УПРАВЛЕНИЕ КЭШЕМ ==========

    /**
     * Выгружает механизмы чанка из кэша
     */
    public void unloadChunkMechanisms(Chunk chunk) {
        ChunkCoordinate chunkCoord = ChunkCoordinate.of(chunk);
        Set<Location> chunkMechanisms = mechanismsByChunk.remove(chunkCoord);

        if (chunkMechanisms != null) {
            for (Location loc : chunkMechanisms) {
                activeMechanisms.remove(loc);
            }

            LogUtil.warn("Выгружено " + chunkMechanisms.size() + " " +
                    mechanismType.getGuiTitle() + " из чанка " +
                    chunk.getX() + ", " + chunk.getZ());
        }
    }

    /**
     * Добавляет механизм в индексы
     */
    public void addMechanismToIndex(T mechanism) {
        Location loc = mechanism.getLocation();
        ChunkCoordinate chunkCoord = ChunkCoordinate.of(loc);

        activeMechanisms.put(loc, mechanism);
        mechanismsByChunk.computeIfAbsent(chunkCoord, k -> new HashSet<>()).add(loc);
    }

    /**
     * Удаляет механизм из индексов
     */
    protected void removeMechanismFromIndex(Location loc) {
        activeMechanisms.remove(loc);

        ChunkCoordinate chunkCoord = ChunkCoordinate.of(loc);
        Set<Location> chunkMechanisms = mechanismsByChunk.get(chunkCoord);
        if (chunkMechanisms != null) {
            chunkMechanisms.remove(loc);
            if (chunkMechanisms.isEmpty()) {
                mechanismsByChunk.remove(chunkCoord);
            }
        }
    }

    // ========== ОСНОВНЫЕ МЕТОДЫ ДЛЯ РАБОТЫ С МЕХАНИЗМАМИ ==========

    /**
     * Сохраняет механизм
     */
    public void saveMechanism(T mechanism) {
        Location loc = mechanism.getLocation();
        Chunk chunk = loc.getChunk();

        if (!activeMechanisms.containsKey(loc)) {
            addMechanismToIndex(mechanism);
        }

        saveMechanismsFromChunk(chunk);
    }

    /**
     * Создает новый механизм
     */
    public T createMechanism(Location location, Player owner) {
        T mechanism = createMechanismInstance(location, owner, 0, getDefaultCapacity(), true);
        addMechanismToIndex(mechanism);
        saveMechanismsFromChunk(location.getChunk());

        plugin.getLogger().info("Создан новый " + mechanismType.getGuiTitle() +
                " на " + location);
        return mechanism;
    }

    /**
     * Удаляет механизм
     */
    public void deleteMechanism(Location location) {
        removeMechanismFromIndex(location);
        saveMechanismsFromChunk(location.getChunk());

        plugin.getLogger().info("Удален " + mechanismType.getGuiTitle() +
                " с " + location);
    }

    /**
     * Получает механизм по локации
     */
    public T getMechanism(Location location) {
        return activeMechanisms.get(location);
    }

    /**
     * Получает механизм по блоку
     */
    public T getMechanism(Block block) {
        return getMechanism(block.getLocation());
    }

    /**
     * Проверяет, является ли блок механизмом данного типа
     */
    public boolean isMechanism(Block block) {
        return activeMechanisms.containsKey(block.getLocation());
    }

    /**
     * Получает все активные механизмы
     */
    public Collection<T> getAllMechanisms() {
        return activeMechanisms.values();
    }

    /**
     * Получает все механизмы в указанном чанке
     */
    public Set<Location> getMechanismsInChunk(Chunk chunk) {
        return mechanismsByChunk.getOrDefault(ChunkCoordinate.of(chunk), new HashSet<>());
    }

    /**
     * Получает статистику по механизмам
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_" + mechanismType.name().toLowerCase(), activeMechanisms.size());
        stats.put("total_chunks_with_" + mechanismType.name().toLowerCase(), mechanismsByChunk.size());
        return stats;
    }

    /**
     * Сохраняет все механизмы (при выключении сервера)
     */
    public void saveAllMechanisms() {
        for (ChunkCoordinate chunkCoord : mechanismsByChunk.keySet()) {
            World world = plugin.getServer().getWorld(chunkCoord.world());
            if (world == null) continue;

            Chunk chunk = world.getChunkAt(chunkCoord.x(), chunkCoord.z());
            saveMechanismsFromChunk(chunk);
        }

        LogUtil.info("Сохранено " + activeMechanisms.size() + " " +
                mechanismType.getGuiTitle());
    }

    /**
     * Возвращает емкость по умолчанию для механизма
     */
    protected int getDefaultCapacity() {
        return 1000; // Можно переопределить в наследниках
    }

    /**
     * Получает уникальный ключ для блока внутри чанка (для обратной совместимости)
     */
    protected String getBlockKey(Block block) {
        int relativeX = block.getX() & 0x0F;
        int relativeZ = block.getZ() & 0x0F;
        int y = block.getY();

        return mechanismType.name().toLowerCase() + "_" + relativeX + "_" + y + "_" + relativeZ;
    }
}