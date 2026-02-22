package org.example.artyom.magicMechanism.linkservice;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.data.Keys;
import org.example.artyom.magicMechanism.data.enums.MechanismType;
import org.example.artyom.magicMechanism.managers.BarrierManager;
import org.example.artyom.magicMechanism.managers.GeneratorManager;
import org.example.artyom.magicMechanism.mechanisms.Barrier;
import org.example.artyom.magicMechanism.mechanisms.BaseMechanism;
import org.example.artyom.magicMechanism.mechanisms.Generator;
import org.example.artyom.magicMechanism.utils.LogUtil;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class GeneratorBarrierService extends BukkitRunnable {

    private final MagicMechanism plugin;
    private final GeneratorManager generatorManager;
    private final BarrierManager barrierManager;
    private final int TRANSFER_RATE = 10; // сколько энергии передаем за раз

    // Карта потребителей с их логикой обработки
    private final Map<MechanismType, ConsumerHandler> consumerHandlers = new EnumMap<>(MechanismType.class);

    // Блоки, которые могут принимать энергию
    private final List<MechanismType> consumers = new ArrayList<>();

    public GeneratorBarrierService(MagicMechanism plugin, GeneratorManager generatorManager, BarrierManager barrierManager) {
        this.plugin = plugin;
        this.generatorManager = generatorManager;
        this.barrierManager = barrierManager;

        // Добавляем типы блоков-потребителей
        consumers.add(MechanismType.BARRIER);

        // Регистрируем обработчики для разных типов
        registerHandlers();
    }

    /**
     * Регистрирует обработчики для разных типов потребителей
     */
    private void registerHandlers() {
        // Обработчик для BARRIER (накопление энергии)
        consumerHandlers.put(MechanismType.BARRIER, (block, energy) -> {
            PersistentDataContainer pdc = ((TileState) block.getState()).getPersistentDataContainer();

            // Получаем текущую накопленную энергию
            Barrier barrier = barrierManager.getBarrier(block);

            int stored = barrier.getEnergyLevel();
            int maxStorage = barrier.getCapacity();

            // Добавляем энергию
            int newStored = Math.min(stored + energy, maxStorage);
            barrier.setEnergyLevel(newStored);

            // Обновляем состояние блока

            LogUtil.info(String.format("Барьер получил %d энергии. Всего: %d/%d",
                    energy, newStored, maxStorage));

            return newStored - stored; // Возвращаем сколько реально использовано
        });
    }

    @Override
    public void run() {
        // Проходим по всем активным генераторам
        for (BaseMechanism generatorRaw : generatorManager.getAllMechanisms()) {
            if (!(generatorRaw instanceof Generator generator)) continue;

            // Генерируем энергию (раскомментируйте если нужно)
            // generator.generateEnergy();

            // Ищем потребителей рядом и передаем энергию
            transferEnergyToNearbyConsumers(generator);

            // Сохраняем обновленное состояние
            generatorManager.saveMechanism(generator);
        }

        // Дополнительно: обновляем активные эффекты у потребителей
        updateActiveConsumers();
    }

    /**
     * Передаем энергию потребителям рядом
     */
    private void transferEnergyToNearbyConsumers(Generator generator) {
        Location loc = generator.getLocation();
        int radius = 5; // радиус поиска потребителей
        int remainingEnergy = generator.getEnergyLevel();

        if (remainingEnergy <= 0) return;

        // Ищем блоки в радиусе
        for (int x = -radius; x <= radius && remainingEnergy > 0; x++) {
            for (int y = -radius; y <= radius && remainingEnergy > 0; y++) {
                for (int z = -radius; z <= radius && remainingEnergy > 0; z++) {
                    Block relativeBlock = loc.clone().add(x, y, z).getBlock();

                    // Проверяем, является ли блок потребителем
                    if (!isConsumer(relativeBlock)) continue;

                    // Получаем тип потребителя
                    MechanismType consumerType = getMechanismType(relativeBlock);
                    if (consumerType == null) continue;

                    // Получаем обработчик для этого типа
                    ConsumerHandler handler = consumerHandlers.get(consumerType);
                    if (handler == null) continue;

                    // Рассчитываем сколько энергии передать (не больше остатка)
                    int energyToTransfer = Math.min(TRANSFER_RATE, remainingEnergy);

                    // Передаем энергию через обработчик
                    int usedEnergy = handler.consume(relativeBlock, energyToTransfer);

                    if (usedEnergy > 0) {
                        // Уменьшаем энергию генератора
                        generator.transferEnergy(usedEnergy);
                        remainingEnergy -= usedEnergy;

                        // Визуальный эффект передачи
                        spawnTransferEffect(generator.getLocation(), relativeBlock.getLocation());

                        LogUtil.info(String.format("Передано %d энергии от генератора к %s",
                                usedEnergy, consumerType));
                    }
                }
            }
        }
    }

    /**
     * Проверяем, может ли блок потреблять энергию
     */
    public boolean isConsumer(Block block) {
        if (block == null) return false;

        MechanismType type = getMechanismType(block);
        if (type == null || !consumers.contains(type)) return false;

        // Дополнительная проверка: может ли потребитель принять энергию
        return canAcceptEnergy(block, type);
    }

    /**
     * Проверяет, может ли конкретный потребитель принять энергию
     */
    private boolean canAcceptEnergy(Block block, MechanismType type) {
        if (!(block.getState() instanceof TileState tile)) return false;

        PersistentDataContainer pdc = tile.getPersistentDataContainer();

        switch (type) {
            case BARRIER:
                Barrier barrier = barrierManager.getBarrier(block);
                int stored = barrier.getEnergyLevel();
                int maxStorage = barrier.getCapacity();
                return stored < maxStorage;

            default:
                return true;
        }
    }

    /**
     * Получает тип механизма из блока
     */
    private MechanismType getMechanismType(Block block) {
        if (!(block.getState() instanceof TileState tileState)) {
            return null;
        }

        PersistentDataContainer pdc = tileState.getPersistentDataContainer();
        String typeName = pdc.get(Keys.MACHINE_TYPE, PersistentDataType.STRING);

        if (typeName == null) return null;

        try {
            return MechanismType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Применяет ускорение к печи
     */

    /**
     * Активирует машину при получении энергии
     */
    private void activateMachine(Block machine) {
        // Здесь можно добавить логику активации
        // Например, включить частицы, звук и т.д.
        machine.getWorld().playSound(machine.getLocation(),
                org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.5f);
    }

    /**
     * Создает визуальный эффект передачи энергии
     */
    private void spawnTransferEffect(Location from, Location to) {
        // Частицы между генератором и потребителем
        from.getWorld().spawnParticle(
                org.bukkit.Particle.END_ROD,
                from.clone().add(0.5, 1, 0.5),
                0, // 0 значит одна частица
                to.getX() - from.getX(),
                to.getY() - from.getY(),
                to.getZ() - from.getZ(),
                0.5
        );
    }

    /**
     * Обновляет активных потребителей (вызывается каждый тик)
     */
    private void updateActiveConsumers() {
        // Здесь можно обновлять эффекты у всех потребителей
        // Например, уменьшать время ускорения у печей
        for (BaseMechanism mechanism : generatorManager.getAllMechanisms()) {
            // Логика обновления
        }
    }

    /**
     * Добавляет новый тип потребителя
     */
    public void addConsumerType(MechanismType type, ConsumerHandler handler) {
        consumers.add(type);
        if (handler != null) {
            consumerHandlers.put(type, handler);
        }
    }

    /**
     * Удаляет тип потребителя
     */
    public void removeConsumerType(MechanismType type) {
        consumers.remove(type);
        consumerHandlers.remove(type);
    }

    /**
     * Интерфейс для обработчиков потребителей
     */
    @FunctionalInterface
    public interface ConsumerHandler {
        /**
         * Обрабатывает получение энергии потребителем
         * @param block блок-потребитель
         * @param energy количество полученной энергии
         * @return сколько энергии реально использовано
         */
        int consume(Block block, int energy);
    }
}