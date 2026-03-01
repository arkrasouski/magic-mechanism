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
import org.example.artyom.magicMechanism.mechanisms.Generator;
import org.example.artyom.magicMechanism.network.EnergyNetwork;
import org.example.artyom.magicMechanism.utils.LogUtil;

import java.util.*;

public class GeneratorBarrierService extends BukkitRunnable {

    private final MagicMechanism plugin;
    private final GeneratorManager generatorManager;
    private final BarrierManager barrierManager;
    private final NetworkManager networkManager;
    private final CableManager cableManager;
    private final int TRANSFER_RATE = 10;
    private final int GENERATION_RATE = 5;
    private final Map<MechanismType, ConsumerHandler> consumerHandlers = new EnumMap<>(MechanismType.class);

    public GeneratorBarrierService(MagicMechanism plugin,
                                   GeneratorManager generatorManager,
                                   BarrierManager barrierManager,
                                   CableManager cableManager,
                                   NetworkManager networkManager, CableManager cableManager1) {
        this.plugin = plugin;
        this.generatorManager = generatorManager;
        this.barrierManager = barrierManager;
        this.networkManager = networkManager;
        this.cableManager = cableManager1;
        registerHandlers();
        LogUtil.info("GeneratorBarrierService инициализирован");
    }

    private void registerHandlers() {
        consumerHandlers.put(MechanismType.BARRIER, (block, energy) -> {
            Barrier barrier = barrierManager.getMechanism(block.getLocation());
            if (barrier == null) {
                LogUtil.warn("  ❌ Барьер не найден в обработчике!");
                return 0;
            }

            int stored = barrier.getEnergyLevel();
            int maxStorage = barrier.getCapacity();

            if (stored >= maxStorage) {
                LogUtil.warn("  ⚠ Барьер уже полностью заряжен: " + stored + "/" + maxStorage);
                return 0;
            }

            int newStored = Math.min(stored + energy, maxStorage);
            barrier.setEnergyLevel(newStored);
            barrierManager.saveMechanism(barrier);

            LogUtil.info(String.format("  ✅ Барьер получил %d энергии. Всего: %d/%d",
                    newStored - stored, newStored, maxStorage));

            return newStored - stored;
        });
    }

    @Override
    public void run() {
        LogUtil.warn("=== ТИК СЕРВИСА ===");
        LogUtil.warn("Всего генераторов: " + generatorManager.getAllMechanisms().size());
        LogUtil.warn("Всего барьеров: " + barrierManager.getAllMechanisms().size());
        LogUtil.warn("Всего сетей: " + networkManager.getAllNetworks().size());

        int totalTransferred = 0;

        for (Generator generator : generatorManager.getAllMechanisms()) {
            int transferred = processGenerator(generator);
            totalTransferred += transferred;
        }

        if (totalTransferred > 0) {
            LogUtil.warn("Итого передано энергии в этом тике: " + totalTransferred);
        }
    }

    private int processGenerator(Generator generator) {
        int energyBefore = generator.getEnergyLevel();
        Location genLoc = generator.getLocation();

        LogUtil.warn("--- Обработка генератора " + formatLocation(genLoc) + " ---");
        LogUtil.warn("  Энергия до: " + energyBefore + "/" + generator.getCapacity());

        // 1. Генерация энергии
        if (energyBefore < generator.getCapacity()) {
            int newEnergy = Math.min(energyBefore + GENERATION_RATE, generator.getCapacity());
            generator.setEnergyLevel(newEnergy);
            LogUtil.warn("  ⚡ Сгенерировано: " + (newEnergy - energyBefore) + ", теперь: " + newEnergy);
        }

        // 2. Передача энергии через сеть
        int currentEnergy = generator.getEnergyLevel();
        int transferred = 0;

        if (currentEnergy > 0) {
            EnergyNetwork network = networkManager.getNetwork(genLoc);

            if (network == null) {
                LogUtil.warn("  ❌ Сеть НЕ НАЙДЕНА для генератора!");

                // Проверим, есть ли кабель рядом
                Set<Location> neighbors = getNeighbors(genLoc);
                LogUtil.warn("  Соседи генератора: " + neighbors.size());
                for (Location neighbor : neighbors) {
                    EnergyNetwork neighborNetwork = networkManager.getNetwork(neighbor);
                    LogUtil.warn("    Сосед " + formatLocation(neighbor) +
                            (neighborNetwork != null ? " в сети " + neighborNetwork.getId() : " без сети"));
                }
            } else {
                LogUtil.warn("  ✅ Сеть найдена: " + network.getId());
                LogUtil.warn("  Узлов в сети: " + network.getNodes().size());
                LogUtil.warn("  Генераторов в сети: " + network.getGenerators().size());
                LogUtil.warn("  Потребителей в сети: " + network.getConsumers().size());

                if (network.isValid()) {
                    LogUtil.warn("  ✅ Сеть валидна");
                    transferred = distributeGeneratorEnergy(generator, network);
                } else {
                    LogUtil.warn("  ❌ Сеть НЕ валидна!");
                    if (network.getGenerators().isEmpty()) LogUtil.warn("    - Нет генераторов в сети");
                    if (network.getConsumers().isEmpty()) LogUtil.warn("    - Нет потребителей в сети");
                    if (!network.isConnected()) LogUtil.warn("    - Сеть не связна");
                }
            }
        }

        // 3. Сохранение изменений
        if (energyBefore != generator.getEnergyLevel() || transferred > 0) {
            generatorManager.saveMechanism(generator);
            LogUtil.warn("  💾 Генератор сохранен");
        }

        return transferred;
    }

    private int distributeGeneratorEnergy(Generator generator, EnergyNetwork network) {
        Location generatorLoc = generator.getLocation();
        int availableEnergy = generator.getEnergyLevel();
        int totalTransferred = 0;

        LogUtil.warn("  === РАСПРЕДЕЛЕНИЕ ЭНЕРГИИ ===");
        LogUtil.warn("    Доступно энергии: " + availableEnergy);

        Set<Location> consumers = network.getConsumers();
        LogUtil.warn("    Потребителей в сети: " + consumers.size());

        if (consumers.isEmpty()) {
            LogUtil.warn("    ❌ Нет потребителей в сети");
            return 0;
        }

        // Сортируем потребителей по расстоянию
        List<Location> sortedConsumers = new ArrayList<>(consumers);
        sortedConsumers.sort(Comparator.comparingDouble(loc -> loc.distance(generatorLoc)));

        for (Location consumerLoc : sortedConsumers) {
            if (availableEnergy <= 0) {
                LogUtil.warn("    ⚡ Энергия закончилась");
                break;
            }

            LogUtil.warn("    --- Проверка потребителя " + formatLocation(consumerLoc) + " ---");

            // Проверяем путь в сети
            boolean hasPath = network.hasPath(generatorLoc, consumerLoc);
            LogUtil.warn("      Есть путь? " + hasPath);

            if (!hasPath) {
                LogUtil.warn("      ❌ Нет пути, пропускаем");
                continue;
            }

            // Получаем информацию о потребителе
            ConsumerInfo consumerInfo = getConsumerInfo(consumerLoc);
            if (consumerInfo == null) {
                LogUtil.warn("      ❌ Не удалось получить информацию о потребителе");
                continue;
            }

            int needed = getNeededEnergy(consumerInfo);
            LogUtil.warn("      Нужно энергии: " + needed);

            if (needed <= 0) {
                LogUtil.warn("      ⚠ Потребитель не нуждается в энергии");
                continue;
            }

            int toTransfer = Math.min(Math.min(TRANSFER_RATE, availableEnergy), needed);
            LogUtil.warn("      Будет передано: " + toTransfer);

            if (toTransfer > 0) {
                // Применяем энергию к потребителю
                int used = consumerInfo.handler.consume(consumerInfo.block, toTransfer);
                LogUtil.warn("      ✅ Передано: " + used);

                if (used > 0) {
                    // Уменьшаем энергию генератора
                    generator.transferEnergy(used);
                    availableEnergy -= used;
                    totalTransferred += used;

                    // Визуальный эффект
                    spawnTransferEffect(generatorLoc, consumerLoc);
                }
            }
        }

        LogUtil.warn("    Итого передано от этого генератора: " + totalTransferred);
        return totalTransferred;
    }

    private Set<Location> getNeighbors(Location loc) {
        Set<Location> neighbors = new HashSet<>();
        int[][] offsets = {{1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1}};

        for (int[] offset : offsets) {
            Location neighbor = loc.clone().add(offset[0], offset[1], offset[2]);
            if (cableManager != null && cableManager.isCable(neighbor.getBlock())) {
                neighbors.add(neighbor);
            }
        }
        return neighbors;
    }

    private ConsumerInfo getConsumerInfo(Location loc) {
        Block block = loc.getBlock();

        LogUtil.warn("      🔍 Проверка потребителя на " + formatLocation(loc));

        // Проверяем барьер
        if (barrierManager.hasMechanism(loc)) {
            LogUtil.warn("        ✓ Барьер найден в BarrierManager");
            Barrier barrier = barrierManager.getMechanism(loc);
            if (barrier != null) {
                LogUtil.warn("        Энергия: " + barrier.getEnergyLevel() + "/" + barrier.getCapacity());
                if (barrier.getEnergyLevel() < barrier.getCapacity()) {
                    LogUtil.warn("        ✅ Может принимать энергию");
                    return new ConsumerInfo(block, MechanismType.BARRIER,
                            consumerHandlers.get(MechanismType.BARRIER));
                } else {
                    LogUtil.warn("        ❌ Уже полностью заряжен");
                }
            } else {
                LogUtil.warn("        ❌ Барьер есть в hasMechanism, но getMechanism вернул null!");
            }
        } else {
            LogUtil.warn("        ❌ Барьер НЕ найден в BarrierManager");
        }

        return null;
    }

    private int getNeededEnergy(ConsumerInfo consumer) {
        if (consumer.type == MechanismType.BARRIER) {
            Barrier barrier = barrierManager.getMechanism(consumer.block.getLocation());
            if (barrier != null) {
                return barrier.getCapacity() - barrier.getEnergyLevel();
            }
        }
        return 0;
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

    private String formatLocation(Location loc) {
        return String.format("[%d %d %d]", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private static class ConsumerInfo {
        final Block block;
        final MechanismType type;
        final ConsumerHandler handler;

        ConsumerInfo(Block block, MechanismType type, ConsumerHandler handler) {
            this.block = block;
            this.type = type;
            this.handler = handler;
        }
    }

    @FunctionalInterface
    public interface ConsumerHandler {
        int consume(Block block, int energy);
    }
}