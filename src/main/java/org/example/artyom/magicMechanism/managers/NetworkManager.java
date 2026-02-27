package org.example.artyom.magicMechanism.managers;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.network.EnergyNetwork;
import org.example.artyom.magicMechanism.utils.LogUtil;

import java.util.*;

public class NetworkManager {
    private final MagicMechanism plugin;
    private final Map<UUID, EnergyNetwork> networks = new HashMap<>();
    private final Map<Location, UUID> locationToNetwork = new HashMap<>();
    private final Set<Location> processedLocations = new HashSet<>();

    private static final BlockFace[] FACES = {
            BlockFace.NORTH, BlockFace.SOUTH,
            BlockFace.EAST, BlockFace.WEST,
            BlockFace.UP, BlockFace.DOWN
    };

    public NetworkManager(MagicMechanism plugin) {
        this.plugin = plugin;
    }

    /**
     * Сканирует и строит сеть от начальной точки
     */
    public EnergyNetwork buildNetwork(Location start) {
        processedLocations.clear();
        UUID networkId = UUID.randomUUID();
        EnergyNetwork network = new EnergyNetwork(networkId);

        LogUtil.warn("=== ПОСТРОЕНИЕ СЕТИ ===");
        LogUtil.warn("Старт: " + start);

        // Используем BFS для поиска всех связанных компонентов
        Queue<Location> queue = new LinkedList<>();
        queue.add(start);
        processedLocations.add(start);

        while (!queue.isEmpty()) {
            Location current = queue.poll();
            Block currentBlock = current.getBlock();

            LogUtil.warn("Обрабатываем: " + current);

            // Определяем тип текущего блока и добавляем в сеть
            if (plugin.getCableManager().isCable(currentBlock)) {
                network.addCable(current);
                LogUtil.warn("  + Кабель");
            }

            if (plugin.getGeneratorManager().hasMechanism(current)) {
                network.addGenerator(current);
                LogUtil.warn("  + Генератор");
            }

            if (plugin.getBarrierManager().hasMechanism(current)) {
                network.addConsumer(current);
                LogUtil.warn("  + Потребитель (Барьер)");
            }

            // Проверяем всех соседей
            for (BlockFace face : FACES) {
                Block neighbor = currentBlock.getRelative(face);
                Location neighborLoc = neighbor.getLocation();

                if (processedLocations.contains(neighborLoc)) {
                    continue;
                }

                // Проверяем, является ли сосед частью сети (кабель, генератор или потребитель)
                boolean isNetworkComponent =
                        plugin.getCableManager().isCable(neighbor) ||
                                plugin.getGeneratorManager().hasMechanism(neighborLoc) ||
                                plugin.getBarrierManager().hasMechanism(neighborLoc);

                if (isNetworkComponent) {
                    // Добавляем соединение в сеть
                    network.addConnection(current, neighborLoc);

                    // Добавляем в очередь для дальнейшей обработки
                    processedLocations.add(neighborLoc);
                    queue.add(neighborLoc);

                    LogUtil.warn("  Связано с: " + neighborLoc);
                }
            }
        }

        // Сохраняем сеть
        networks.put(networkId, network);
        for (Location loc : processedLocations) {
            locationToNetwork.put(loc, networkId);
        }

        LogUtil.warn("=== СЕТЬ ПОСТРОЕНА ===");
        LogUtil.warn("ID: " + networkId);
        LogUtil.warn("Кабелей: " + network.getCables().size());
        LogUtil.warn("Генераторов: " + network.getGenerators().size());
        LogUtil.warn("Потребителей: " + network.getConsumers().size());
        LogUtil.warn("Валидна: " + network.isValid());

        return network;
    }

    /**
     * Проверяет валидность всей сети
     */
    public boolean isNetworkValid(Location component) {
        UUID networkId = locationToNetwork.get(component);
        if (networkId == null) {
            return false;
        }

        EnergyNetwork network = networks.get(networkId);
        if (network == null) {
            return false;
        }

        // Перестраиваем сеть для проверки актуальности
        EnergyNetwork rebuiltNetwork = buildNetwork(component);

        // Сравниваем с сохраненной сетью
        return rebuiltNetwork.isValid();
    }

    /**
     * Проверяет возможность передачи от генератора к потребителю
     */
    public boolean canTransfer(Location generator, Location consumer) {
        UUID generatorNetwork = locationToNetwork.get(generator);
        UUID consumerNetwork = locationToNetwork.get(consumer);

        // Должны быть в одной сети
        if (generatorNetwork == null || consumerNetwork == null ||
                !generatorNetwork.equals(consumerNetwork)) {
            return false;
        }

        EnergyNetwork network = networks.get(generatorNetwork);
        if (network == null) {
            return false;
        }

        return network.canTransfer(generator, consumer);
    }

    /**
     * Обновляет сеть после изменений (размещение/удаление)
     */
    public void updateNetwork(Location changed) {
        LogUtil.warn("=== ОБНОВЛЕНИЕ СЕТИ ===");
        LogUtil.warn("Изменение на: " + changed);

        // Находим все затронутые сети
        Set<UUID> affectedNetworks = new HashSet<>();

        // Добавляем сеть измененного блока (если есть)
        UUID oldNetwork = locationToNetwork.get(changed);
        if (oldNetwork != null) {
            affectedNetworks.add(oldNetwork);
        }

        // Добавляем сети соседних блоков
        Block changedBlock = changed.getBlock();
        for (BlockFace face : FACES) {
            Block neighbor = changedBlock.getRelative(face);
            UUID neighborNetwork = locationToNetwork.get(neighbor.getLocation());
            if (neighborNetwork != null) {
                affectedNetworks.add(neighborNetwork);
            }
        }

        // Перестраиваем затронутые сети
        for (UUID networkId : affectedNetworks) {
            networks.remove(networkId);
            // Удаляем все локации из маппинга для этой сети
            locationToNetwork.entrySet().removeIf(entry -> entry.getValue().equals(networkId));
        }

        // Строим новые сети от всех измененных точек
        Set<Location> toProcess = new HashSet<>();
        toProcess.add(changed);

        for (BlockFace face : FACES) {
            toProcess.add(changedBlock.getRelative(face).getLocation());
        }

        for (Location loc : toProcess) {
            if (isNetworkComponent(loc) && !locationToNetwork.containsKey(loc)) {
                buildNetwork(loc);
            }
        }
    }

    private boolean isNetworkComponent(Location loc) {
        Block block = loc.getBlock();
        return plugin.getCableManager().isCable(block) ||
                plugin.getGeneratorManager().hasMechanism(loc) ||
                plugin.getBarrierManager().hasMechanism(loc);
    }

    /**
     * Получает сеть для компонента
     */
    public EnergyNetwork getNetwork(Location component) {
        UUID networkId = locationToNetwork.get(component);
        return networkId != null ? networks.get(networkId) : null;
    }
}
