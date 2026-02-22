package org.example.artyom.magicMechanism.managers;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.data.enums.MechanismType;
import org.example.artyom.magicMechanism.mechanisms.Barrier;

import java.util.UUID;

public class BarrierManager extends BaseManager {
    public BarrierManager(MagicMechanism plugin) {
        super(plugin, MechanismType.BARRIER);
    }

    public Barrier loadBarrier(Block block, Player owner) {
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

        return new Barrier(block.getLocation(),loadedOwner, energy, maxEnergy, isActive);
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
    public Barrier getBarrier(Block block) {
        // Сначала проверяем кэш
        Barrier barrier = (Barrier) activeMechanisms.get(block.getLocation());
        if (barrier != null) {
            return barrier;
        }

        // Если нет в кэше, пробуем загрузить
        barrier = loadBarrier(block, null);
        if (barrier != null) {
            activeMechanisms.put(block.getLocation(), barrier);
        }

        return barrier;
    }

    /**
     * Создаем новый генератор
     */
    public void createBarrier(Block block, Player owner) {
        Barrier barrier = new Barrier(block.getLocation(), owner);
        saveMechanism(barrier);
    }


}
