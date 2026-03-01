package org.example.artyom.magicMechanism.network;

import org.bukkit.Location;
import org.example.artyom.magicMechanism.mechanisms.Barrier;
import org.example.artyom.magicMechanism.mechanisms.Cable;
import org.example.artyom.magicMechanism.mechanisms.Generator;
import org.example.artyom.magicMechanism.utils.LogUtil;

import java.util.*;

public class EnergyNetwork {
    private final UUID networkId;
    private final Set<Location> nodes = new HashSet<>(); // Все узлы сети (кабели)
    private final Set<Location> generators = new HashSet<>();
    private final Set<Location> consumers = new HashSet<>();
    private final Map<Location, Set<Location>> graph = new HashMap<>(); // Полный граф сети

    private static final int TRANSFER_RATE = 10;

    public EnergyNetwork(UUID networkId) {
        this.networkId = networkId;
    }

    // ========== УПРАВЛЕНИЕ УЗЛАМИ ==========

    public void addNode(Location loc, String type) {
        nodes.add(loc);
        switch (type) {
            case "generator" -> generators.add(loc);
            case "consumer" -> consumers.add(loc);
        }
    }

    public void removeNode(Location loc) {
        nodes.remove(loc);
        generators.remove(loc);
        consumers.remove(loc);
        graph.remove(loc);
        // Удаляем все ссылки на этот узел
        graph.values().forEach(set -> set.remove(loc));
    }

    // ========== ПОСТРОЕНИЕ ГРАФА ==========

    /**
     * Строит граф на основе прямых соединений кабелей
     * Вызывается NetworkManager после изменений
     */
    // В EnergyNetwork.java, метод rebuildGraph
    public void rebuildGraph(Map<Location, Set<Location>> neighborData) {
        LogUtil.warn("=== EnergyNetwork.rebuildGraph ===");
        LogUtil.warn("  Получено данных о соседях: " + neighborData.size());

        graph.clear();

        for (Map.Entry<Location, Set<Location>> entry : neighborData.entrySet()) {
            Location node = entry.getKey();
            LogUtil.warn("  Обработка узла " + formatLocation(node));

            if (!nodes.contains(node)) {
                LogUtil.warn("    ⚠ Узел не в списке nodes! Пропускаем");
                continue;
            }

            for (Location neighbor : entry.getValue()) {
                LogUtil.warn("    Сосед: " + formatLocation(neighbor));
                if (nodes.contains(neighbor)) {
                    graph.computeIfAbsent(node, k -> new HashSet<>()).add(neighbor);
                    graph.computeIfAbsent(neighbor, k -> new HashSet<>()).add(node);
                    LogUtil.warn("      ✅ Добавлена двунаправленная связь");
                } else {
                    LogUtil.warn("      ❌ Сосед не в nodes");
                }
            }
        }

        LogUtil.warn("  Итоговый граф имеет " + graph.size() + " узлов");
    }
    private String formatLocation(Location loc) {
        return String.format("[%d %d %d]", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
    // ========== ПРОВЕРКА СВЯЗНОСТИ ==========
    /**
     * Проверяет, существует ли путь между двумя локациями
     */
    public boolean hasPath(Location from, Location to) {
        if (!nodes.contains(from) || !nodes.contains(to)) return false;
        if (from.equals(to)) return true;

        Set<Location> visited = new HashSet<>();
        Queue<Location> queue = new LinkedList<>();

        queue.add(from);
        visited.add(from);

        while (!queue.isEmpty()) {
            Location current = queue.poll();

            for (Location neighbor : graph.getOrDefault(current, new HashSet<>())) {
                if (!visited.contains(neighbor)) {
                    if (neighbor.equals(to)) {
                        return true;
                    }
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return false;
    }

    public boolean isConnected() {
        if (nodes.isEmpty()) return false;

        Set<Location> visited = new HashSet<>();
        dfs(nodes.iterator().next(), visited);

        return visited.containsAll(nodes);
    }

    private void dfs(Location current, Set<Location> visited) {
        if (visited.contains(current)) return;
        visited.add(current);

        for (Location neighbor : graph.getOrDefault(current, new HashSet<>())) {
            dfs(neighbor, visited);
        }
    }

    // ========== ПОИСК ПУТИ ==========

    public List<Location> findPath(Location from, Location to) {
        if (!nodes.contains(from) || !nodes.contains(to)) return Collections.emptyList();

        Map<Location, Location> previous = new HashMap<>();
        Queue<Location> queue = new LinkedList<>();
        Set<Location> visited = new HashSet<>();

        queue.add(from);
        visited.add(from);

        while (!queue.isEmpty()) {
            Location current = queue.poll();

            for (Location neighbor : graph.getOrDefault(current, new HashSet<>())) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    previous.put(neighbor, current);

                    if (neighbor.equals(to)) {
                        return reconstructPath(previous, from, to);
                    }

                    queue.add(neighbor);
                }
            }
        }

        return Collections.emptyList();
    }

    private List<Location> reconstructPath(Map<Location, Location> previous, Location start, Location end) {
        List<Location> path = new ArrayList<>();
        Location current = end;

        while (current != null) {
            path.add(0, current);
            current = previous.get(current);
        }

        return path;
    }

    // ========== РАСПРЕДЕЛЕНИЕ ЭНЕРГИИ ==========

    public void distributeEnergy(Map<Location, Integer> generatorEnergy,
                                 Map<Location, Integer> consumerCapacity,
                                 Map<Location, Integer> consumerCurrent) {

        int totalEnergy = generatorEnergy.values().stream().mapToInt(Integer::intValue).sum();
        int totalDemand = consumerCapacity.values().stream().mapToInt(Integer::intValue).sum();

        if (totalEnergy == 0 || totalDemand == 0) return;

        double ratio = Math.min(1.0, (double) totalEnergy / totalDemand);

        for (Location consumer : consumers) {
            int demand = consumerCapacity.getOrDefault(consumer, 0);
            int allocated = (int) (demand * ratio);

            if (allocated > 0) {
                // Находим ближайший генератор с энергией
                Location source = findNearestGenerator(consumer, generatorEnergy);
                if (source != null) {
                    // Здесь NetworkManager реально передаст энергию через кабели
                    // А мы только возвращаем, сколько нужно передать и куда
                    int available = generatorEnergy.getOrDefault(source, 0);
                    int toTransfer = Math.min(allocated, available);

                    // Обновляем энергию генератора (логика в менеджере)
                    // Обновляем энергию потребителя (логика в менеджере)
                }
            }
        }
    }

    private Location findNearestGenerator(Location target, Map<Location, Integer> generatorEnergy) {
        return generators.stream()
                .filter(loc -> generatorEnergy.getOrDefault(loc, 0) > 0)
                .min(Comparator.comparingDouble(a -> a.distance(target)))
                .orElse(null);
    }
    /**
     * Проверяет, валидна ли сеть (есть генераторы, потребители и все связано)
     */
    public boolean isValid() {
        return !generators.isEmpty() && !consumers.isEmpty() && isConnected();
    }
    // ========== ГЕТТЕРЫ ==========

    public UUID getId() { return networkId; }
    public Set<Location> getNodes() { return Collections.unmodifiableSet(nodes); }
    public Set<Location> getGenerators() { return Collections.unmodifiableSet(generators); }
    public Set<Location> getConsumers() { return Collections.unmodifiableSet(consumers); }
    public Map<Location, Set<Location>> getGraph() { return Collections.unmodifiableMap(graph); }
}