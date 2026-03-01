package org.example.artyom.magicMechanism.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.example.artyom.magicMechanism.mechanisms.Cable;
import org.example.artyom.magicMechanism.mechanisms.Generator;
import org.example.artyom.magicMechanism.mechanisms.Barrier;
import org.example.artyom.magicMechanism.network.EnergyNetwork;
import org.example.artyom.magicMechanism.utils.LogUtil;

import java.util.*;
import java.util.stream.Collectors;

public class NetworkManager {
    private final Map<UUID, EnergyNetwork> networks = new HashMap<>();
    private final Map<Location, EnergyNetwork> locationToNetwork = new HashMap<>();
    private final Map<Location, Set<Location>> neighborCache = new HashMap<>(); // Кэш соседей

    private final CableManager cableManager;
    private final GeneratorManager generatorManager;
    private final BarrierManager barrierManager;

    public NetworkManager(CableManager cableManager, GeneratorManager generatorManager, BarrierManager barrierManager) {
        this.cableManager = cableManager;
        this.generatorManager = generatorManager;
        this.barrierManager = barrierManager;
    }

    // ========== СОБЫТИЯ РАЗМЕЩЕНИЯ ==========

    // В NetworkManager.java, обновите onCablePlaced
    public void onCablePlaced(Cable cable, Location location) {
        LogUtil.warn("========== NetworkManager.onCablePlaced ==========");
        LogUtil.warn("Кабель: " + formatLocation(location));

        // Проверяем, есть ли уже сеть для этой локации
        EnergyNetwork existingNetwork = locationToNetwork.get(location);
        if (existingNetwork != null) {
            LogUtil.warn("⚠ Кабель УЖЕ в сети: " + existingNetwork.getId());
            LogUtil.warn("  Пропускаем создание новой сети");
            LogUtil.warn("===============================================");
            return;
        }
        LogUtil.warn("✅ Кабель еще не в сети");

        // Получаем соседей
        Set<Location> neighbors = cable.getDirectConnections();
        LogUtil.warn("Соседей найдено: " + neighbors.size());

        for (Location neighbor : neighbors) {
            LogUtil.warn("  Сосед: " + formatLocation(neighbor));

            // Проверяем, есть ли у соседа сеть
            EnergyNetwork neighborNetwork = locationToNetwork.get(neighbor);
            if (neighborNetwork != null) {
                LogUtil.warn("    ✅ У соседа ЕСТЬ сеть: " + neighborNetwork.getId());
            } else {
                LogUtil.warn("    ❌ У соседа НЕТ сети");

                // Дополнительная проверка - может сосед просто не в locationToNetwork?
                Cable neighborCable = cableManager.getCable(neighbor);
                if (neighborCable != null) {
                    LogUtil.warn("    Но сосед - кабель! Его сеть: " +
                            (neighborCable.getNetwork() != null ? neighborCable.getNetwork().getId() : "null"));
                }
            }
        }

        // Сохраняем в кэш
        neighborCache.put(location, neighbors);

        // Находим соседние сети
        Set<EnergyNetwork> adjacentNetworks = new HashSet<>();
        for (Location neighbor : neighbors) {
            EnergyNetwork net = locationToNetwork.get(neighbor);
            if (net != null) {
                adjacentNetworks.add(net);
                LogUtil.warn("  Добавлена соседняя сеть: " + net.getId());
            }
        }

        LogUtil.warn("Найдено соседних сетей: " + adjacentNetworks.size());

        // Определяем целевую сеть
        EnergyNetwork targetNetwork;

        if (adjacentNetworks.isEmpty()) {
            LogUtil.warn("⚠ Нет соседних сетей! Причины:");
            LogUtil.warn("  1. Нет соседей вообще");
            LogUtil.warn("  2. У соседей нет сетей (они не в locationToNetwork)");
            LogUtil.warn("  3. Соседи не зарегистрированы в менеджерах");

            // Проверим все соседние блоки напрямую
            LogUtil.warn("  Проверка всех соседей через менеджеры:");
            BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
                    BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};

            for (BlockFace face : faces) {
                Block neighborBlock = location.getBlock().getRelative(face);
                Location neighborLoc = neighborBlock.getLocation();

                boolean isCable = cableManager.isCable(neighborBlock);
                boolean isGenerator = generatorManager.hasMechanism(neighborLoc);
                boolean isBarrier = barrierManager.hasMechanism(neighborLoc);

                if (isCable || isGenerator || isBarrier) {
                    LogUtil.warn("    Блок " + formatLocation(neighborLoc) +
                            " является механизмом, но не в locationToNetwork!");
                }
            }

            targetNetwork = new EnergyNetwork(UUID.randomUUID());
            networks.put(targetNetwork.getId(), targetNetwork);
            LogUtil.warn("✅ СОЗДАНА НОВАЯ СЕТЬ: " + targetNetwork.getId());
        } else if (adjacentNetworks.size() == 1) {
            targetNetwork = adjacentNetworks.iterator().next();
            LogUtil.warn("✅ ИСПОЛЬЗУЕМ СУЩЕСТВУЮЩУЮ СЕТЬ: " + targetNetwork.getId());
        } else {
            targetNetwork = mergeNetworks(adjacentNetworks);
            LogUtil.warn("✅ ОБЪЕДИНЕНЫ СЕТИ В: " + targetNetwork.getId());
        }

        // Добавляем кабель в сеть
        targetNetwork.addNode(location, "cable");
        locationToNetwork.put(location, targetNetwork);
        cable.setNetwork(targetNetwork);

        LogUtil.warn("✅ Кабель добавлен в сеть " + targetNetwork.getId());
        LogUtil.warn("  Узлов в сети теперь: " + targetNetwork.getNodes().size());

        // Проверяем, что добавление прошло успешно
        EnergyNetwork checkNetwork = locationToNetwork.get(location);
        LogUtil.warn("  Проверка locationToNetwork: " +
                (checkNetwork != null ? checkNetwork.getId() : "null"));

        rebuildNetworkGraph(targetNetwork);
        LogUtil.warn("===============================================");
    }

    public void onGeneratorPlaced(Generator generator, Location location) {
        // Находим сеть через соседние кабели
        EnergyNetwork network = findNetworkViaNeighbors(location);
        if (network != null) {
            network.addNode(location, "generator");
            locationToNetwork.put(location, network);
            rebuildNetworkGraph(network);
        }
    }

    public void onGeneratorRemoved(Location location){

        EnergyNetwork network = findNetworkViaNeighbors(location);
        if (network != null) {
            network.removeNode(location);
            locationToNetwork.remove(location);
            rebuildNetworkGraph(network);
        }
    }

    public void onConsumerPlaced(Barrier consumer, Location location) {
        EnergyNetwork network = findNetworkViaNeighbors(location);
        if (network != null) {
            network.addNode(location, "consumer");
            locationToNetwork.put(location, network);
            rebuildNetworkGraph(network);
        }
    }

    public void onConsumerRemoved(Location location){
        EnergyNetwork network = findNetworkViaNeighbors(location);
        if (network != null) {
            network.removeNode(location);
            locationToNetwork.remove(location);
            rebuildNetworkGraph(network);
        }
    }

    // ========== СОБЫТИЯ УДАЛЕНИЯ ==========

    public void onCableRemoved(Location location) {
        EnergyNetwork network = locationToNetwork.get(location);
        if (network == null) return;

        // Удаляем из сети
        network.removeNode(location);
        neighborCache.remove(location);
        locationToNetwork.remove(location);

        // Проверяем, не распалась ли сеть
        checkAndSplitNetwork(network);
    }

    // ========== ПОСТРОЕНИЕ ГРАФА ==========

    private void rebuildNetworkGraph(EnergyNetwork network) {
        LogUtil.warn("=== ПЕРЕСТРОЙКА ГРАФА СЕТИ " + network.getId() + " ===");

        Map<Location, Set<Location>> networkNeighbors = new HashMap<>();

        for (Location node : network.getNodes()) {
            Set<Location> neighbors = neighborCache.get(node);
            LogUtil.warn("  Узел " + formatLocation(node) + " имеет соседей в кэше: " +
                    (neighbors != null ? neighbors.size() : "null"));

            if (neighbors != null) {
                Set<Location> networkNeighborsSet = new HashSet<>();
                for (Location neighbor : neighbors) {
                    if (network.getNodes().contains(neighbor)) {
                        networkNeighborsSet.add(neighbor);
                        LogUtil.warn("    ➡ Добавляем связь с " + formatLocation(neighbor));
                    } else {
                        LogUtil.warn("    ⚠ Сосед " + formatLocation(neighbor) + " не в этой сети!");
                    }
                }
                networkNeighbors.put(node, networkNeighborsSet);
            }
        }

        network.rebuildGraph(networkNeighbors);

        // Проверяем получившийся граф
        LogUtil.warn("  Построенный граф:");
        for (Map.Entry<Location, Set<Location>> entry : network.getGraph().entrySet()) {
            LogUtil.warn("    " + formatLocation(entry.getKey()) + " -> " +
                    entry.getValue().stream().map(this::formatLocation).collect(Collectors.joining(", ")));
        }
    }
    public void debugNetworkState() {
        LogUtil.warn("=== СОСТОЯНИЕ СЕТЕЙ ===");
        LogUtil.warn("Всего сетей: " + networks.size());

        for (Map.Entry<UUID, EnergyNetwork> entry : networks.entrySet()) {
            EnergyNetwork net = entry.getValue();
            LogUtil.warn("  Сеть " + net.getId() + ":");
            LogUtil.warn("    Узлов: " + net.getNodes().size());
            for (Location node : net.getNodes()) {
                LogUtil.warn("      " + formatLocation(node));
            }
        }

        LogUtil.warn("=== locationToNetwork ===");
        LogUtil.warn("Записей: " + locationToNetwork.size());
        for (Map.Entry<Location, EnergyNetwork> entry : locationToNetwork.entrySet()) {
            LogUtil.warn("  " + formatLocation(entry.getKey()) + " -> " +
                    entry.getValue().getId());
        }
    }
    private String formatLocation(Location loc) {
        return String.format("[%d %d %d]", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    // ========== ОБЪЕДИНЕНИЕ СЕТЕЙ ==========

    private EnergyNetwork mergeNetworks(Set<EnergyNetwork> networksToMerge) {
        // Берем первую как основную
        Iterator<EnergyNetwork> iterator = networksToMerge.iterator();
        EnergyNetwork main = iterator.next();

        while (iterator.hasNext()) {
            EnergyNetwork other = iterator.next();

            // Переносим все узлы
            for (Location node : other.getNodes()) {
                main.addNode(node, getNodeType(node));
                locationToNetwork.put(node, main);
            }

            // Удаляем старую сеть
            networks.remove(other.getId());
        }

        rebuildNetworkGraph(main);
        return main;
    }

    // ========== РАЗДЕЛЕНИЕ СЕТИ ==========

    private void checkAndSplitNetwork(EnergyNetwork network) {
        if (!network.isConnected()) {
            LogUtil.warn("Сеть распадается на части!");

            // Находим компоненты связности
            List<Set<Location>> components = findConnectedComponents(network);

            if (components.size() > 1) {
                // Удаляем старую сеть
                networks.remove(network.getId());

                // Создаем новые сети для каждого компонента
                for (Set<Location> component : components) {
                    EnergyNetwork newNetwork = new EnergyNetwork(UUID.randomUUID());

                    for (Location node : component) {
                        newNetwork.addNode(node, getNodeType(node));
                        locationToNetwork.put(node, newNetwork);
                    }

                    rebuildNetworkGraph(newNetwork);
                    networks.put(newNetwork.getId(), newNetwork);

                    LogUtil.warn("Создана новая сеть: " + newNetwork.getId() +
                            " с " + component.size() + " узлами");
                }
            }
        }
    }

    private List<Set<Location>> findConnectedComponents(EnergyNetwork network) {
        Set<Location> unvisited = new HashSet<>(network.getNodes());
        List<Set<Location>> components = new ArrayList<>();

        while (!unvisited.isEmpty()) {
            Location start = unvisited.iterator().next();
            Set<Location> component = new HashSet<>();

            dfsComponent(start, component, unvisited, network);
            components.add(component);
        }

        return components;
    }

    private void dfsComponent(Location current, Set<Location> component,
                              Set<Location> unvisited, EnergyNetwork network) {
        if (!unvisited.contains(current)) return;

        unvisited.remove(current);
        component.add(current);

        for (Location neighbor : network.getGraph().getOrDefault(current, new HashSet<>())) {
            dfsComponent(neighbor, component, unvisited, network);
        }
    }

    // ========== ПЕРЕДАЧА ЭНЕРГИИ ==========

    public void tick() {
        // Каждый тик распределяем энергию во всех сетях
        for (EnergyNetwork network : networks.values()) {
            distributeEnergyInNetwork(network);
        }
    }

    private void distributeEnergyInNetwork(EnergyNetwork network) {
        // Собираем данные о генераторах
        Map<Location, Integer> generatorEnergy = new HashMap<>();
        for (Location genLoc : network.getGenerators()) {
            Generator gen = generatorManager.getMechanism(genLoc);
            if (gen != null) {
                generatorEnergy.put(genLoc, gen.getEnergyLevel());
            }
        }

        // Собираем данные о потребителях
        Map<Location, Integer> consumerCapacity = new HashMap<>();
        Map<Location, Integer> consumerCurrent = new HashMap<>();
        for (Location consLoc : network.getConsumers()) {
            Barrier barrier = barrierManager.getMechanism(consLoc);
            if (barrier != null) {
                consumerCapacity.put(consLoc, barrier.getCapacity() - barrier.getEnergyLevel());
                consumerCurrent.put(consLoc, barrier.getEnergyLevel());
            }
        }

        // Распределяем энергию (логика в сети)
        network.distributeEnergy(generatorEnergy, consumerCapacity, consumerCurrent);

        // Здесь можно добавить реальную передачу энергии по путям
        // Но это уже детали реализации
    }

    // ========== ХЕЛПЕРЫ ==========

    private EnergyNetwork findNetworkViaNeighbors(Location location) {
        // Сканируем соседей через соответствующий менеджер
        for (Location neighbor : getNeighbors(location)) {
            EnergyNetwork network = locationToNetwork.get(neighbor);
            if (network != null) {
                return network;
            }
        }
        return null;
    }

    private Set<Location> getNeighbors(Location location) {
        Set<Location> neighbors = new HashSet<>();

        // Проверяем все 6 направлений
        int[][] offsets = {{1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1}};

        for (int[] offset : offsets) {
            Location neighbor = location.clone().add(offset[0], offset[1], offset[2]);

            if (cableManager.isCable(neighbor.getBlock()) ||
                    generatorManager.hasMechanism(neighbor) ||
                    barrierManager.hasMechanism(neighbor)) {
                neighbors.add(neighbor);
            }
        }

        return neighbors;
    }

    private String getNodeType(Location loc) {
        if (generatorManager.hasMechanism(loc)) return "generator";
        if (barrierManager.hasMechanism(loc)) return "consumer";
        return "cable";
    }
    public void onCableNeighborsChanged(Location location, Set<Location> newNeighbors) {
        LogUtil.warn("NetworkManager: изменение соседей кабеля " + location);

        // Обновляем кэш соседей
        neighborCache.put(location, newNeighbors);

        // Находим сеть кабеля
        EnergyNetwork network = locationToNetwork.get(location);
        if (network != null) {
            // Перестраиваем граф сети
            rebuildNetworkGraph(network);

            // Проверяем, не изменилась ли связность
            if (!network.isConnected()) {
               checkAndSplitNetwork(network);
            }
        }
    }

    public EnergyNetwork getNetwork(Location loc) {
        return locationToNetwork.get(loc);
    }

    public Map<UUID, EnergyNetwork>  getAllNetworks(){
        return networks;
    }
}
