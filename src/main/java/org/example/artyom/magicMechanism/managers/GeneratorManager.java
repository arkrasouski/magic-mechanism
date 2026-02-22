package org.example.artyom.magicMechanism.managers;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.mechanisms.Generator;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GeneratorManager {
    private final MagicMechanism plugin;
    private final NamespacedKey energyKey;
    private final NamespacedKey maxEnergyKey;
    private final NamespacedKey activeKey;
    private final NamespacedKey ownerKey;

    // Кэш активных генераторов для быстрого доступа
    private final Map<Location, Generator> activeGenerators = new ConcurrentHashMap<>();

    public GeneratorManager(MagicMechanism plugin) {
        this.plugin = plugin;

        // Создаем ключи для хранения в PDC
        this.energyKey = new NamespacedKey(plugin, "generator_energy");
        this.maxEnergyKey = new NamespacedKey(plugin, "generator_max_energy");
        this.activeKey = new NamespacedKey(plugin, "generator_active");
        this.ownerKey = new NamespacedKey(plugin, "generator_owner");
    }

    /**
     * Сохраняем генератор в PDC чанка
     */
    public void saveGenerator(Generator generator) {
        Location loc = generator.getLocation();
        Block block = loc.getBlock();
        Chunk chunk = block.getChunk();

        // Получаем PDC чанка
        PersistentDataContainer chunkData = chunk.getPersistentDataContainer();

        // Создаем уникальный ключ для этого блока в чанке
        String blockKey = getBlockKey(block);

        // Сохраняем все параметры генератора
        NamespacedKey energyKey = new NamespacedKey(plugin, blockKey + "_energy");
        NamespacedKey maxEnergyKey = new NamespacedKey(plugin, blockKey + "_max");
        NamespacedKey activeKey = new NamespacedKey(plugin, blockKey + "_active");
        NamespacedKey ownerKey = new NamespacedKey(plugin, blockKey + "_owner");

        chunkData.set(energyKey, PersistentDataType.INTEGER, generator.getEnergyLevel());
        chunkData.set(maxEnergyKey, PersistentDataType.INTEGER, generator.getCapacity());
        chunkData.set(activeKey, PersistentDataType.INTEGER, generator.isActive() ? 1 : 0);

        // Сохраняем UUID владельца как строку
        if (generator.getOwner() != null) {
            chunkData.set(ownerKey, PersistentDataType.STRING, generator.getOwner().getUniqueId().toString());
        }

        // Обновляем кэш
        activeGenerators.put(loc, generator);
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
     * Удаляем генератор из PDC
     */
    public void removeGenerator(Block block) {
        Chunk chunk = block.getChunk();
        PersistentDataContainer chunkData = chunk.getPersistentDataContainer();

        String blockKey = getBlockKey(block);

        // Удаляем все ключи этого генератора
        chunkData.remove(new NamespacedKey(plugin, blockKey + "_energy"));
        chunkData.remove(new NamespacedKey(plugin, blockKey + "_max"));
        chunkData.remove(new NamespacedKey(plugin, blockKey + "_active"));
        chunkData.remove(new NamespacedKey(plugin, blockKey + "_owner"));

        // Удаляем из кэша
        activeGenerators.remove(block.getLocation());
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
    public Generator getGenerator(Block block) {
        // Сначала проверяем кэш
        Generator generator = activeGenerators.get(block.getLocation());
        if (generator != null) {
            return generator;
        }

        // Если нет в кэше, пробуем загрузить
        generator = loadGenerator(block, null);
        if (generator != null) {
            activeGenerators.put(block.getLocation(), generator);
        }

        return generator;
    }

    /**
     * Создаем новый генератор
     */
    public void createGenerator(Block block, Player owner) {
        Generator generator = new Generator(block.getLocation(), owner);
        saveGenerator(generator);
    }

    /**
     * Получаем уникальный ключ для блока внутри чанка
     */
    private String getBlockKey(Block block) {
        // Используем относительные координаты в чанке (0-15)
        int relativeX = block.getX() & 0x0F; // Берем последние 4 бита
        int relativeZ = block.getZ() & 0x0F;
        int y = block.getY();

        return "gen_" + relativeX + "_" + y + "_" + relativeZ;
    }

    /**
     * Получаем все активные генераторы
     */
    public Collection<Generator> getAllGenerators() {
        return activeGenerators.values();
    }
}
