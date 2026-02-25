package org.example.artyom.magicMechanism.linkservice;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;
import org.example.artyom.magicMechanism.MagicMechanism;
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
    private final int TRANSFER_RATE = 10;
    private final int GENERATION_RATE = 5;

    private final Map<MechanismType, ConsumerHandler> consumerHandlers = new EnumMap<>(MechanismType.class);
    //private final List<MechanismType> consumers = new ArrayList<>();

    public GeneratorBarrierService(MagicMechanism plugin, GeneratorManager generatorManager, BarrierManager barrierManager) {
        this.plugin = plugin;
        this.generatorManager = generatorManager;
        this.barrierManager = barrierManager;

        //consumers.add(MechanismType.BARRIER);
        registerHandlers();
    }

    private void registerHandlers() {
        //привязка типа и обработчика для типа
        consumerHandlers.put(MechanismType.BARRIER, (block, energy) -> {
            Barrier barrier = barrierManager.getMechanism(block.getLocation());

            if (barrier == null) {
                return 0;
            }

            int stored = barrier.getEnergyLevel();
            int maxStorage = barrier.getCapacity();

            if (stored >= maxStorage) {
                return 0;
            }

            int newStored = Math.min(stored + energy, maxStorage);
            barrier.setEnergyLevel(newStored);
            barrierManager.saveMechanism(barrier);

            LogUtil.info(String.format("Барьер получил %d энергии. Всего: %d/%d",
                    energy, newStored, maxStorage));

            return newStored - stored;
        });
    }


//    Для каждого генератора в мире:
//
//    Проверяет текущий уровень энергии
//
//    Если есть энергия (currentEnergy > 0):
//
//    Пытается передать её ближайшим потребителям
//
//    Сохраняет изменения в генераторе


    @Override
    public void run() {
        boolean anyEnergyTransferred = false;

        for (BaseMechanism generatorRaw : generatorManager.getAllMechanisms()) {
            if (!(generatorRaw instanceof Generator generator)) continue;

            int energyBefore = generator.getEnergyLevel();
            LogUtil.warn("Генератор " + generator.getLocation() + " энергия до: " + energyBefore);

            // Генерируем энергию ТОЛЬКО если не достигнут максимум
//            if (energyBefore < generator.getCapacity()) {
//                int newEnergy = Math.min(energyBefore + GENERATION_RATE, generator.getCapacity());
//                generator.setEnergyLevel(newEnergy);
//                LogUtil.warn("Сгенерирована энергия: " + (newEnergy - energyBefore) + ", теперь: " + newEnergy);
//            }

            // Передаем энергию ТОЛЬКО если есть что передавать
            int currentEnergy = generator.getEnergyLevel();
            if (currentEnergy > 0) {
                int energyTransferred = transferEnergyToNearbyConsumers(generator); //Проверка каждого блока - может ли он принять энергию
                if (energyTransferred > 0) {
                    anyEnergyTransferred = true;
                    LogUtil.warn("Передано энергии: " + energyTransferred + ", осталось: " + generator.getEnergyLevel());
                }
            } else {
                LogUtil.warn("Нет энергии для передачи");
            }

            // Сохраняем только если энергия изменилась
            if (energyBefore != generator.getEnergyLevel()) {
                generatorManager.saveMechanism(generator);
            }
        }

        if (anyEnergyTransferred) {
            LogUtil.warn("Энергия передавалась в этом тике");
        }
    }

    /**
     * Передает энергию потребителям
     * @return количество переданной энергии
     */
    private int transferEnergyToNearbyConsumers(Generator generator) {
        Location loc = generator.getLocation();
        int radius = 5;
        int remainingEnergy = generator.getEnergyLevel();
        int totalTransferred = 0;

        if (remainingEnergy <= 0) return 0;

        // Сначала собираем всех потребителей, которые могут принять энергию
        List<ConsumerInfo> availableConsumers = new ArrayList<>();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block relativeBlock = loc.clone().add(x, y, z).getBlock();

                    if (relativeBlock.getLocation().equals(loc)) continue;

                    // Проверяем, является ли блок потребителем
                    ConsumerInfo consumer = getConsumerIfCanAccept(relativeBlock); //Проверка потребителей
                    if (consumer != null) {
                        availableConsumers.add(consumer);
                    }
                }
            }
        }

        if (availableConsumers.isEmpty()) {
            LogUtil.warn("Нет доступных потребителей");
            return 0;
        }

        LogUtil.warn("Найдено потребителей: " + availableConsumers.size());

        // Передаем энергию потребителям
        for (ConsumerInfo consumer : availableConsumers) {
            if (remainingEnergy <= 0) break;

            int energyToTransfer = Math.min(TRANSFER_RATE, remainingEnergy);
            int usedEnergy = consumer.handler.consume(consumer.block, energyToTransfer);

            if (usedEnergy > 0) {
                generator.transferEnergy(usedEnergy);
                remainingEnergy -= usedEnergy;
                totalTransferred += usedEnergy;
                spawnTransferEffect(generator.getLocation(), consumer.block.getLocation());

                LogUtil.info(String.format("Передано %d энергии от генератора к %s",
                        usedEnergy, consumer.type));
            }
        }

        return totalTransferred;
    }

    /**
     * Проверяет, может ли блок потреблять энергию, и возвращает информацию о потребителе
     */
    private ConsumerInfo getConsumerIfCanAccept(Block block) {
        if (block == null) return null;

        Location loc = block.getLocation();
        // Сейчас проверяет только барьеры:
        // Проверяем барьер
        if (barrierManager.hasMechanism(loc)) { //Спрашивает у BarrierManager: "Есть ли на этих координатах зарегистрированный барьер?"
            Barrier barrier = barrierManager.getMechanism(loc);
            if (barrier != null && barrier.getEnergyLevel() < barrier.getCapacity()) {
                return new ConsumerInfo(block, MechanismType.BARRIER, consumerHandlers.get(MechanismType.BARRIER)); //Обработчик, который знает, как именно этот тип потребляет энергию
            }
        }

        // Здесь можно добавить другие типы потребителей

        return null;
    }

    /**
     * Проверяем, может ли блок потреблять энергию
     */
    public boolean isConsumer(Block block) {
        return getConsumerIfCanAccept(block) != null;
    }

    /**
     * Получает тип механизма из менеджеров
     */
    private MechanismType getMechanismType(Block block) {
        if (block == null) return null;

        Location loc = block.getLocation();

        if (barrierManager.hasMechanism(loc)) {
            return MechanismType.BARRIER;
        }

        if (generatorManager.hasMechanism(loc)) {
            return MechanismType.GENERATOR;
        }

        return null;
    }

    private void spawnTransferEffect(Location from, Location to) {
        try {
            from.getWorld().spawnParticle(
                    org.bukkit.Particle.END_ROD,
                    from.clone().add(0.5, 1, 0.5),
                    0,
                    to.getX() - from.getX(),
                    to.getY() - from.getY(),
                    to.getZ() - from.getZ(),
                    0.5
            );
        } catch (Exception e) {
            // Игнорируем ошибки частиц
        }
    }

    public void addConsumerType(MechanismType type, ConsumerHandler handler) {
        //consumers.add(type);
        if (handler != null) {
            consumerHandlers.put(type, handler);
        }
    }

    public void removeConsumerType(MechanismType type) {
       // consumers.remove(type);
        consumerHandlers.remove(type);
    }

    /**
     * Вспомогательный класс для хранения информации о потребителе
     */
    private static class ConsumerInfo {
        final Block block;
        final MechanismType type;
        final ConsumerHandler handler;

        ConsumerInfo(Block block, MechanismType type, ConsumerHandler handler) {
            this.block = block;
            this.type = type;
            this.handler = handler; //Обработчик, который знает, как именно этот тип потребляет энергию
        }
    }

    @FunctionalInterface //Действие для механизма
    public interface ConsumerHandler {
        int consume(Block block, int energy);
    }
}
