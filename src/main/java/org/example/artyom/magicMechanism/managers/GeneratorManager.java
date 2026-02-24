package org.example.artyom.magicMechanism.managers;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.data.enums.MechanismType;
import org.example.artyom.magicMechanism.data.records.ChunkCoordinate;
import org.example.artyom.magicMechanism.data.records.GeneratorData;
import org.example.artyom.magicMechanism.mechanisms.Generator;
import org.example.artyom.magicMechanism.utils.LogUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GeneratorManager extends BaseManager{
    private final NamespacedKey generatorCountKey;
    private final NamespacedKey generatorListKey;
    private final Map<Location, Generator> activeGenerators; // Кэш активных генераторов
    private final Map<ChunkCoordinate, Set<Location>> generatorsByChunk; // Индекс генераторов по чанкам


    public GeneratorManager(MagicMechanism plugin) {
        super(plugin, MechanismType.GENERATOR);

        this.activeGenerators = new ConcurrentHashMap<>();
        this.generatorsByChunk = new ConcurrentHashMap<>();

        // Инициализация ключей для хранения списка генераторов в чанке
        this.generatorCountKey = new NamespacedKey(plugin, "generator_count");
        this.generatorListKey = new NamespacedKey(plugin, "generator_list");
        loadAllGeneratorsFromLoadedChunks();
    }


    /**
     * Загружаем генератор из PDC чанка
     */
    public Generator loadGenerator(Block block, Player owner) {
        Chunk chunk = block.getChunk();
        PersistentDataContainer chunkData = chunk.getPersistentDataContainer();

        String blockKey = getBlockKey(block);

        NamespacedKey energyKey = new NamespacedKey(plugin, blockKey + "_energy");
        NamespacedKey maxEnergyKey = new NamespacedKey(plugin, blockKey + "_max");
        NamespacedKey activeKey = new NamespacedKey(plugin, blockKey + "_active");
        NamespacedKey ownerKey = new NamespacedKey(plugin, blockKey + "_owner");

        // Проверяем, есть ли данные для этого блока
        if (!chunkData.has(energyKey, PersistentDataType.INTEGER)) {
            return null; // Это не генератор
        }

        // Загружаем данные
        int energy = chunkData.getOrDefault(energyKey, PersistentDataType.INTEGER, 0);
        int maxEnergy = chunkData.getOrDefault(maxEnergyKey, PersistentDataType.INTEGER, 1000);
        boolean isActive = chunkData.getOrDefault(activeKey, PersistentDataType.INTEGER, 1) == 1;

        // Загружаем владельца
        Player loadedOwner = owner;
        if (chunkData.has(ownerKey, PersistentDataType.STRING)) {
            String ownerUUID = chunkData.get(ownerKey, PersistentDataType.STRING);
            try {
                UUID uuid = UUID.fromString(ownerUUID);
                loadedOwner = plugin.getServer().getPlayer(uuid);
            } catch (IllegalArgumentException ignored) {}
        }

        return new Generator(block.getLocation(),loadedOwner, energy, maxEnergy, isActive);
    }

    /**
     * Загружаем все генераторы из чанка (при загрузке чанка)
     */

    /**
     * Получаем генератор по блоку
     */
//    public Generator getGenerator(Block block) {
//        // Сначала проверяем кэш
//        Generator generator = (Generator) activeMechanisms.get(block.getLocation());
//        if (generator != null) {
//            return generator;
//        }
//
//        // Если нет в кэше, пробуем загрузить
//        generator = loadGenerator(block, null);
//        if (generator != null) {
//            activeMechanisms.put(block.getLocation(), generator);
//        }
//
//        return generator;
//    }

    /**
     * Создаем новый генератор
     */
    public Generator createGenerator(Block block, Player owner) {
        Generator generator = new Generator(block.getLocation(), owner);
        saveMechanism(generator);
        return generator;
    }

//================================================================
public void loadGeneratorsFromChunk(Chunk chunk) {
    ChunkCoordinate chunkCoord = ChunkCoordinate.of(chunk);
    PersistentDataContainer chunkData = chunk.getPersistentDataContainer();

    // Получаем количество генераторов в чанке
    int generatorCount = chunkData.getOrDefault(generatorCountKey, PersistentDataType.INTEGER, 0);
    if (generatorCount == 0) return;

    // Получаем список генераторов в виде строки
    String generatorsString = chunkData.get(generatorListKey, PersistentDataType.STRING);
    if (generatorsString == null || generatorsString.isEmpty()) return;

    // Разбираем строку на отдельные записи
    String[] generatorEntries = generatorsString.split("\\|");
    Set<Location> chunkGenerators = new HashSet<>();

    for (String entry : generatorEntries) {
        if (entry.isEmpty()) continue;

        GeneratorData data = GeneratorData.deserialize(entry);
        if (data == null) continue;

        Location loc = new Location(chunk.getWorld(), data.x(), data.y(), data.z());

        // Проверяем, что блок действительно существует
        Block block = loc.getBlock();

        // Создаем генератор из сохраненных данных
        Player owner = data.owner() != null ? plugin.getServer().getPlayer(data.owner()) : null;
        Generator generator = new Generator(loc,owner, data.energy(), data.maxEnergy(), data.active());

        // Добавляем в кэш
        activeGenerators.put(loc, generator);
        chunkGenerators.add(loc);

        LogUtil.warn("Загружен генератор из чанка: " + loc);
    }

    // Сохраняем индекс
    generatorsByChunk.put(chunkCoord, chunkGenerators);

    LogUtil.info("Загружено " + chunkGenerators.size() +
            " генераторов из чанка " + chunk.getX() + ", " + chunk.getZ());
}

    /**
     * Сохраняет все генераторы указанного чанка
     */
    public void saveGeneratorsFromChunk(Chunk chunk) {
        ChunkCoordinate chunkCoord = ChunkCoordinate.of(chunk);
        Set<Location> chunkGenerators = generatorsByChunk.get(chunkCoord);

        if (chunkGenerators == null || chunkGenerators.isEmpty()) {
            // Нет генераторов в этом чанке - очищаем данные
            clearChunkGeneratorData(chunk);
            return;
        }

        PersistentDataContainer chunkData = chunk.getPersistentDataContainer();

        // Сериализуем все генераторы чанка
        List<String> serializedGenerators = new ArrayList<>();

        for (Location loc : chunkGenerators) {
            Generator generator = activeGenerators.get(loc);
            if (generator == null) continue;

            // Создаем данные для сохранения
            GeneratorData data = new GeneratorData(
                    loc.getBlockX(),
                    loc.getBlockY(),
                    loc.getBlockZ(),
                    generator.getEnergyLevel(),
                    generator.getCapacity(),
                    generator.isActive(),
                    generator.getOwner() != null ? generator.getOwner().getUniqueId() : null
            );

            serializedGenerators.add(data.serialize());
        }

        // Сохраняем в PDC чанка
        String generatorsString = String.join("|", serializedGenerators);
        chunkData.set(generatorListKey, PersistentDataType.STRING, generatorsString);
        chunkData.set(generatorCountKey, PersistentDataType.INTEGER, serializedGenerators.size());

        LogUtil.warn("Сохранено " + serializedGenerators.size() +
                " генераторов в чанк " + chunk.getX() + ", " + chunk.getZ());
    }

    /**
     * Очищает данные о генераторах в чанке
     */
    private void clearChunkGeneratorData(Chunk chunk) {
        PersistentDataContainer chunkData = chunk.getPersistentDataContainer();
        chunkData.remove(generatorCountKey);
        chunkData.remove(generatorListKey);
    }

    /**
     * Загружает все генераторы из всех загруженных чанков (при старте)
     */
    public void loadAllGeneratorsFromLoadedChunks() {
        for (World world : plugin.getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                loadGeneratorsFromChunk(chunk);
            }
        }
    }

    // ========== УПРАВЛЕНИЕ КЭШЕМ ==========

    /**
     * Очищает кэш для выгруженного чанка
     */
    public void unloadChunkGenerators(Chunk chunk) {
        ChunkCoordinate chunkCoord = ChunkCoordinate.of(chunk);
        Set<Location> chunkGenerators = generatorsByChunk.remove(chunkCoord);

        if (chunkGenerators != null) {
            // Удаляем генераторы из активного кэша
            for (Location loc : chunkGenerators) {
                activeGenerators.remove(loc);
            }

            LogUtil.warn("Выгружено " + chunkGenerators.size() +
                    " генераторов из чанка " + chunk.getX() + ", " + chunk.getZ());
        }
    }

    /**
     * Добавляет генератор в индексы
     */
    public void addGeneratorToIndex(Generator generator) {
        Location loc = generator.getLocation();
        ChunkCoordinate chunkCoord = ChunkCoordinate.of(loc);

        // Добавляем в активный кэш
        activeGenerators.put(loc, generator);

        // Добавляем в индекс по чанкам
        generatorsByChunk.computeIfAbsent(chunkCoord, k -> new HashSet<>()).add(loc);
    }

    /**
     * Удаляет генератор из индексов
     */
    public void removeGeneratorFromIndex(Location loc) {
        activeGenerators.remove(loc);

        ChunkCoordinate chunkCoord = ChunkCoordinate.of(loc);
        Set<Location> chunkGenerators = generatorsByChunk.get(chunkCoord);
        if (chunkGenerators != null) {
            chunkGenerators.remove(loc);
            if (chunkGenerators.isEmpty()) {
                generatorsByChunk.remove(chunkCoord);
            }
        }
    }

    // ========== СЛУШАТЕЛИ СОБЫТИЙ ЧАНКОВ ==========



    // ========== МЕТОДЫ ДЛЯ РАБОТЫ С ГЕНЕРАТОРАМИ ==========

    /**
     * Сохраняет генератор
     */
    public void saveGenerator(Generator generator) {
        Location loc = generator.getLocation();
        Chunk chunk = loc.getChunk();

        // Добавляем в индексы если его там нет
        if (!activeGenerators.containsKey(loc)) {
            addGeneratorToIndex(generator);
        }

        // Сохраняем в PDC чанка
        saveGeneratorsFromChunk(chunk);
    }

    /**
     * Создает новый генератор
     */
    public void createGenerator(Location loc, Player owner) {
        Generator generator = new Generator(loc, owner);
        addGeneratorToIndex(generator);

        // Сохраняем в чанк
        saveGeneratorsFromChunk(loc.getChunk());

        plugin.getLogger().info("Создан новый генератор на " + loc);
    }

    /**
     * Удаляет генератор
     */
    public void deleteGenerator(Location loc) {
        removeGeneratorFromIndex(loc);

        // Обновляем данные в чанке
        saveGeneratorsFromChunk(loc.getChunk());

        plugin.getLogger().info("Удален генератор с " + loc);
    }

    /**
     * Получает генератор по локации
     */
    public Generator getGenerator(Location loc) {
        return activeGenerators.get(loc);
    }

    /**
     * Получает генератор по блоку
     */
    public Generator getGenerator(Block block) {
        return getGenerator(block.getLocation());
    }

    /**
     * Проверяет, является ли блок генератором
     */
    public boolean isGenerator(Block block) {
        return activeGenerators.containsKey(block.getLocation());
    }

    /**
     * Получает все активные генераторы
     */
    public Collection<Generator> getAllGenerators() {
        return activeGenerators.values();
    }

    /**
     * Получает все генераторы в указанном чанке
     */
    public Set<Location> getGeneratorsInChunk(Chunk chunk) {
        return generatorsByChunk.getOrDefault(ChunkCoordinate.of(chunk), new HashSet<>());
    }

    /**
     * Получает статистику по генераторам
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_generators", activeGenerators.size());
        stats.put("total_chunks_with_generators", generatorsByChunk.size());

        int totalEnergy = activeGenerators.values().stream()
                .mapToInt(Generator::getEnergyLevel).sum();
        stats.put("total_energy", totalEnergy);

        return stats;
    }

    /**
     * Сохраняет все генераторы (при выключении сервера)
     */
    public void saveAllGenerators() {
        for (ChunkCoordinate chunkCoord : generatorsByChunk.keySet()) {
            World world = plugin.getServer().getWorld(chunkCoord.world());
            if (world == null) continue;

            Chunk chunk = world.getChunkAt(chunkCoord.x(), chunkCoord.z());
            saveGeneratorsFromChunk(chunk);
        }

        plugin.getLogger().info("Сохранено " + activeGenerators.size() + " генераторов");
    }

}
