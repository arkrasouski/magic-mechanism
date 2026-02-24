package org.example.artyom.magicMechanism.managers;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.data.enums.MechanismType;
import org.example.artyom.magicMechanism.data.records.ChunkCoordinate;
import org.example.artyom.magicMechanism.mechanisms.BaseMechanism;
import org.example.artyom.magicMechanism.mechanisms.Generator;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseManager {
    protected final MagicMechanism plugin;
    private final NamespacedKey energyKey;
    private final NamespacedKey capacityKey;
    private final NamespacedKey activeKey;
    private final NamespacedKey ownerKey;
    private final MechanismType mechanismType;

    private final Map<Location, BaseMechanism> mechanismsByLocation;

    // Индекс по чанкам
    private final Map<ChunkCoordinate, Set<Location>> mechanismsByChunk;

    // Индекс по типу для быстрого доступа
    private final Map<MechanismType, Set<Location>> mechanismsByType;

    // Ключи для PDC
    private final NamespacedKey mechanismsDataKey;
    private final NamespacedKey mechanismsCountKey;



    // Кэш активных генераторов для быстрого доступа
    protected final Map<Location, BaseMechanism> activeMechanisms = new ConcurrentHashMap<>();

    public BaseManager(MagicMechanism plugin, MechanismType mechanismType) {
        this.plugin = plugin;
        this.mechanismType = mechanismType;
        // Создаем ключи для хранения в PDC
        this.energyKey = new NamespacedKey(plugin, mechanismType.name() + "_energy");
        this.capacityKey = new NamespacedKey(plugin, mechanismType.name() + "_capacity");
        this.activeKey = new NamespacedKey(plugin, mechanismType.name() + "_" + "_active");
        this.ownerKey = new NamespacedKey(plugin, mechanismType.name() + "_owner");

        this.mechanismsByLocation = new ConcurrentHashMap<>();
        this.mechanismsByChunk = new ConcurrentHashMap<>();
        this.mechanismsByType = new ConcurrentHashMap<>();

        this.mechanismsDataKey = new NamespacedKey(plugin, "mechanisms_data");
        this.mechanismsCountKey = new NamespacedKey(plugin, "mechanisms_count");
    }

    /**
     * Сохраняем генератор в PDC чанка
     */
    public void saveMechanism(BaseMechanism mechanism) {
        Location loc = mechanism.getLocation();
        Block block = loc.getBlock();
        Chunk chunk = block.getChunk();

        // Получаем PDC чанка
        PersistentDataContainer chunkData = chunk.getPersistentDataContainer();

        // Создаем уникальный ключ для этого блока в чанке
        String blockKey = getBlockKey(block);

        // Сохраняем все параметры генератора
        NamespacedKey energyKey = new NamespacedKey(plugin, mechanismType.name() + "_" + blockKey + "_energy");
        NamespacedKey capacityKey = new NamespacedKey(plugin, mechanismType.name() + "_" + blockKey + "_max");
        NamespacedKey activeKey = new NamespacedKey(plugin, mechanismType.name() + "_" + blockKey + "_active");
        NamespacedKey ownerKey = new NamespacedKey(plugin, mechanismType.name() + "_" + blockKey + "_owner");

        chunkData.set(energyKey, PersistentDataType.INTEGER, mechanism.getEnergyLevel());
        chunkData.set(capacityKey, PersistentDataType.INTEGER, mechanism.getCapacity());
        chunkData.set(activeKey, PersistentDataType.INTEGER, mechanism.isActive() ? 1 : 0);

        // Сохраняем UUID владельца как строку
        if (mechanism.getOwner() != null) {
            chunkData.set(ownerKey, PersistentDataType.STRING, mechanism.getOwner().getUniqueId().toString());
        }

        // Обновляем кэш
        activeMechanisms.put(loc, mechanism);
    }

    /**
     * Загружаем генератор из PDC чанка
     */
//    public Generator loadGenerator(Block block, Player owner) {
//        Chunk chunk = block.getChunk();
//        PersistentDataContainer chunkData = chunk.getPersistentDataContainer();
//
//        String blockKey = getBlockKey(block);
//
//        NamespacedKey energyKey = new NamespacedKey(plugin, mechanismType.name() + "_" + blockKey + "_energy");
//        NamespacedKey capacityKey = new NamespacedKey(plugin, mechanismType.name() + "_" + blockKey + "_max");
//        NamespacedKey activeKey = new NamespacedKey(plugin, mechanismType.name() + "_" + blockKey + "_active");
//        NamespacedKey ownerKey = new NamespacedKey(plugin, mechanismType.name() + "_" + blockKey + "_owner");
//
//        // Проверяем, есть ли данные для этого блока
//        if (!chunkData.has(energyKey, PersistentDataType.INTEGER)) {
//            return null; // Это не генератор
//        }
//
//        // Загружаем данные
//        int energy = chunkData.getOrDefault(energyKey, PersistentDataType.INTEGER, 0);
//        int maxEnergy = chunkData.getOrDefault(capacityKey, PersistentDataType.INTEGER, 1000);
//        boolean isActive = chunkData.getOrDefault(activeKey, PersistentDataType.INTEGER, 1) == 1;
//
//        // Загружаем владельца
//        Player loadedOwner = owner;
//        if (chunkData.has(ownerKey, PersistentDataType.STRING)) {
//            String ownerUUID = chunkData.get(ownerKey, PersistentDataType.STRING);
//            try {
//                UUID uuid = UUID.fromString(ownerUUID);
//                loadedOwner = plugin.getServer().getPlayer(uuid);
//            } catch (IllegalArgumentException ignored) {}
//        }
//
//        return new Generator(block.getLocation(),loadedOwner, energy, maxEnergy, isActive);
//    }

    /**
     * Удаляем генератор из PDC
     */
    public void removeGenerator(Block block) {
        Chunk chunk = block.getChunk();
        PersistentDataContainer chunkData = chunk.getPersistentDataContainer();

        String blockKey = getBlockKey(block);

        // Удаляем все ключи этого генератора
        chunkData.remove(new NamespacedKey(plugin, mechanismType.name() + "_" + blockKey + "_energy"));
        chunkData.remove(new NamespacedKey(plugin, mechanismType.name() + "_" + blockKey + "_max"));
        chunkData.remove(new NamespacedKey(plugin, mechanismType.name() + "_" + blockKey + "_active"));
        chunkData.remove(new NamespacedKey(plugin, mechanismType.name() + "_" + blockKey + "_owner"));

        // Удаляем из кэша
        activeMechanisms.remove(block.getLocation());
    }

    /**
     * Загружаем все генераторы из чанка (при загрузке чанка)
     */
    public void loadGeneratorsFromChunk(Chunk chunk) {
        // Это сложнее, так как нам нужно перебрать все возможные ключи
        // Мы не можем просто получить список всех ключей из PDC :(
        // Поэтому мы будем загружать генераторы по мере необходимости
        // или хранить отдельный список местоположений всех генераторов
    }

    /**
     * Получаем генератор по блоку
     */
//    public Generator getGenerator(Block block) {
//        // Сначала проверяем кэш
//        Generator generator = activeGenerators.get(block.getLocation());
//        if (generator != null) {
//            return generator;
//        }
//
//        // Если нет в кэше, пробуем загрузить
//        generator = loadGenerator(block, null);
//        if (generator != null) {
//            activeGenerators.put(block.getLocation(), generator);
//        }
//
//        return generator;
//    }

    /**
     * Создаем новый генератор
     */
//    public void createGenerator(Block block, Player owner) {
//        Generator generator = new Generator(block.getLocation(), owner);
//        saveGenerator(generator);
//    }

    /**
     * Получаем уникальный ключ для блока внутри чанка
     */
    protected String getBlockKey(Block block) {
        // Используем относительные координаты в чанке (0-15)
        int relativeX = block.getX() & 0x0F; // Берем последние 4 бита
        int relativeZ = block.getZ() & 0x0F;
        int y = block.getY();

        return "gen_" + relativeX + "_" + y + "_" + relativeZ;
    }

    /**
     * Получаем все активные генераторы
     */
    public Collection<BaseMechanism> getAllMechanisms() {
        return activeMechanisms.values();
    }
}
