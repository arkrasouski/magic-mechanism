package org.example.artyom.magicMechanism.linkservice;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.data.enums.MechanismType;
import org.example.artyom.magicMechanism.managers.BarrierManager;
import org.example.artyom.magicMechanism.managers.CableManager;
import org.example.artyom.magicMechanism.managers.GeneratorManager;
import org.example.artyom.magicMechanism.managers.NetworkManager;
import org.example.artyom.magicMechanism.mechanisms.Barrier;
import org.example.artyom.magicMechanism.mechanisms.BaseMechanism;
import org.example.artyom.magicMechanism.mechanisms.Cable;
import org.example.artyom.magicMechanism.mechanisms.Generator;
import org.example.artyom.magicMechanism.network.EnergyNetwork;
import org.example.artyom.magicMechanism.utils.LogUtil;

import java.util.*;

public class GeneratorBarrierService extends BukkitRunnable {

    private final MagicMechanism plugin;
    private final GeneratorManager generatorManager;
    private final BarrierManager barrierManager;
    private final NetworkManager networkManager;
    private final int TRANSFER_RATE = 10;
    private final int GENERATION_RATE = 5;
    private final int CABLE_SEARCH_RADIUS = 10; // Радиус поиска кабелей
    private final CableManager cableManager;
    private final Map<MechanismType, ConsumerHandler> consumerHandlers = new EnumMap<>(MechanismType.class);

    //private final List<MechanismType> consumers = new ArrayList<>();

    public GeneratorBarrierService(MagicMechanism plugin, GeneratorManager generatorManager, BarrierManager barrierManager, CableManager cableManager, NetworkManager networkManager) {
        this.plugin = plugin;
        this.generatorManager = generatorManager;
        this.barrierManager = barrierManager;
        this.cableManager = cableManager;
        this.networkManager = networkManager;
        //consumers.add(MechanismType.BARRIER);
        registerHandlers();
        LogUtil.info("GeneratorBarrierService инициализирован с поддержкой проводов");
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

        for (Generator generator : generatorManager.getAllMechanisms()) {
            int energyBefore = generator.getEnergyLevel();

            // Генерация энергии
            if (energyBefore < generator.getCapacity()) {
                int newEnergy = Math.min(energyBefore + GENERATION_RATE, generator.getCapacity());
                generator.setEnergyLevel(newEnergy);
            }

            // Передача энергии
            int currentEnergy = generator.getEnergyLevel();
            if (currentEnergy > 0) {
                // ✅ Проверяем валидность сети перед передачей
                EnergyNetwork network = networkManager.getNetwork(generator.getLocation());

                if (network != null && network.isValid()) {
                    int energyTransferred = transferEnergyInNetwork(generator, network);
                    if (energyTransferred > 0) {
                        anyEnergyTransferred = true;
                    }
                } else {
                    LogUtil.warn("Сеть невалидна или отсутствует для генератора " + generator.getLocation());
                }
            }

            // Сохранение
            if (energyBefore != generator.getEnergyLevel()) {
                generatorManager.saveMechanism(generator);
            }
        }
    }
    private int transferEnergyInNetwork(Generator generator, EnergyNetwork network) {
        Location generatorLoc = generator.getLocation();
        int remainingEnergy = generator.getEnergyLevel();
        int totalTransferred = 0;

        // Получаем всех потребителей в сети
        Set<Location> consumers = network.getConsumers();
        Set<Location> processedConsumers = new HashSet<>();

        for (Location consumerLoc : consumers) {
            if (remainingEnergy <= 0) break;
            if (processedConsumers.contains(consumerLoc)) continue;

            // ✅ Проверяем, существует ли путь в текущей конфигурации
            if (!network.canTransfer(generatorLoc, consumerLoc)) {
                LogUtil.warn("Нет пути от генератора к потребителю " + consumerLoc);
                continue;
            }

            // Проверяем, может ли потребитель принять энергию
            Block consumerBlock = consumerLoc.getBlock();
            ConsumerInfo consumer = getConsumerIfCanAccept(consumerBlock);

            if (consumer != null) {
                int energyToTransfer = Math.min(TRANSFER_RATE, remainingEnergy);
                int usedEnergy = consumer.handler.consume(consumerBlock, energyToTransfer);

                if (usedEnergy > 0) {
                    generator.transferEnergy(usedEnergy);
                    remainingEnergy -= usedEnergy;
                    totalTransferred += usedEnergy;
                    processedConsumers.add(consumerLoc);

                    // Визуальный эффект
                    spawnTransferEffect(generatorLoc, consumerLoc);
                }
            }
        }

        return totalTransferred;
    }
    /**
     * Передает энергию потребителям
     * @return количество переданной энергии
     */
    private int transferEnergyThroughCables(Generator generator) {
        Location generatorLoc = generator.getLocation();
        int remainingEnergy = generator.getEnergyLevel();
        int totalTransferred = 0;

        Set<Cable> nearbyCables = findNearbyCables(generatorLoc);
        Set<Location> processedConsumers = new HashSet<>();

        for (Cable cable : nearbyCables) {
            if (remainingEnergy <= 0) break;

            // ПРИНУДИТЕЛЬНОЕ СКАНИРОВАНИЕ ПЕРЕД ИСПОЛЬЗОВАНИЕМ
            if (!cableManager.isCable(cable.getLocation().getBlock())) {
                LogUtil.warn("Кабель больше не существует, пропускаем");
                continue;
            }
            Block cableBlock = cable.getLocation().getBlock();
            cable.scanConnections(cableBlock);

            // Добавим задержку для отладки
            try {
                Thread.sleep(10); // Небольшая задержка для последовательности логирования
            } catch (InterruptedException e) {}

            // Получаем всех потребителей, подключенных к этому кабелю
            Set<Location> consumers = cable.getConnectedConsumers();
            LogUtil.warn("Кабель нашел потребителей: " + consumers.size());

            if (consumers.isEmpty()) {
                continue;
            }

            for (Location consumerLoc : consumers) {
                if (remainingEnergy <= 0) break;
                if (processedConsumers.contains(consumerLoc)) continue;

                Block consumerBlock = consumerLoc.getBlock();
                ConsumerInfo consumer = getConsumerIfCanAccept(consumerBlock);

                if (consumer != null) {
                    int energyToTransfer = Math.min(TRANSFER_RATE, remainingEnergy);
                    int usedEnergy = consumer.handler.consume(consumerBlock, energyToTransfer);

                    if (usedEnergy > 0) {
                        generator.transferEnergy(usedEnergy);
                        remainingEnergy -= usedEnergy;
                        totalTransferred += usedEnergy;
                        processedConsumers.add(consumerLoc);
                        spawnCableTransferEffect(generatorLoc, cable.getLocation(), consumerLoc);
                    }
                }
            }
        }

        return totalTransferred;
    }

    private boolean isValidConnection(Cable cable, Location consumerLoc, Location generatorLoc) {
        Block cableBlock = cable.getLocation().getBlock();
        Block consumerBlock = consumerLoc.getBlock();

        // 1. Проверяем, что потребитель действительно adjacent к кабелю
        if (!areBlocksAdjacent(cableBlock, consumerBlock)) {
            LogUtil.warn("Потребитель не adjacent к кабелю");
            return false;
        }

        // 2. Проверяем, что кабель все еще существует
        if (!cableManager.isCable(cableBlock)) {
            LogUtil.warn("Кабель больше не существует");
            return false;
        }

        // 3. Проверяем, что потребитель все еще существует
        if (barrierManager.getMechanism(consumerLoc) == null) {
            LogUtil.warn("Потребитель больше не существует");
            return false;
        }

        // 4. Проверяем, что генератор все еще существует
        if (generatorManager.getMechanism(generatorLoc) == null) {
            LogUtil.warn("Генератор больше не существует");
            return false;
        }

        return true;
    }

    private boolean areBlocksAdjacent(Block cableBlock, Block consumerBlock) {
        if (cableBlock == null || consumerBlock == null) return false;

        int dx = Math.abs(cableBlock.getX() - consumerBlock.getX());
        int dy = Math.abs(cableBlock.getY() - consumerBlock.getY());
        int dz = Math.abs(cableBlock.getZ() - consumerBlock.getZ());

        // Блоки adjacent если разница по одной оси = 1, а по остальным = 0
        return (dx == 1 && dy == 0 && dz == 0) ||
                (dx == 0 && dy == 1 && dz == 0) ||
                (dx == 0 && dy == 0 && dz == 1);
    }

    // Временный метод для проверки
    private void checkBarrierDistance(Location cableLoc) {
        LogUtil.warn("=== ПРОВЕРКА РАССТОЯНИЙ ДО БАРЬЕРОВ ===");

        for (Barrier barrier : barrierManager.getAllMechanisms()) {
            Location barrierLoc = barrier.getLocation();
            double distance = cableLoc.distance(barrierLoc);

            LogUtil.warn("Барьер на " + barrierLoc +
                    " расстояние: " + distance + " блоков");

            // Проверяем, являются ли они соседними
            boolean isAdjacent = Math.abs(cableLoc.getBlockX() - barrierLoc.getBlockX()) <= 1 &&
                    Math.abs(cableLoc.getBlockY() - barrierLoc.getBlockY()) <= 1 &&
                    Math.abs(cableLoc.getBlockZ() - barrierLoc.getBlockZ()) <= 1;

            LogUtil.warn("  Соседний блок? " + isAdjacent);

            if (isAdjacent) {
                LogUtil.warn("  ✅ Барьер должен быть найден при сканировании!");
            }
        }
    }
    /**
     * Находит кабели рядом с локацией
     */
        private Set<Cable> findNearbyCables(Location center) {
            Set<Cable> cables = new HashSet<>();
            int radius = CABLE_SEARCH_RADIUS;

            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        Location checkLoc = center.clone().add(x, y, z);
                        Block block = checkLoc.getBlock();

                        Cable cable = cableManager.getCable(block);
                        if (cable != null) {
                            cables.add(cable);
                        }
                    }
                }
            }

            return cables;
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
    private void spawnCableTransferEffect(Location from, Location cableLoc, Location to) {
        try {
            // Эффект от генератора к кабелю
            from.getWorld().spawnParticle(
                    org.bukkit.Particle.END_ROD,
                    from.clone().add(0.5, 1, 0.5),
                    0,
                    cableLoc.getX() - from.getX(),
                    cableLoc.getY() - from.getY(),
                    cableLoc.getZ() - from.getZ(),
                    0.3
            );

            // Эффект от кабеля к потребителю
            from.getWorld().spawnParticle(
                    org.bukkit.Particle.END_ROD,
                    cableLoc.clone().add(0.5, 0.5, 0.5),
                    0,
                    to.getX() - cableLoc.getX(),
                    to.getY() - cableLoc.getY(),
                    to.getZ() - cableLoc.getZ(),
                    0.3
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
    public void onBlockChanged(Location location) {
        networkManager.updateNetwork(location);
    }
    @FunctionalInterface //Действие для механизма
    public interface ConsumerHandler {
        int consume(Block block, int energy);
    }
}
