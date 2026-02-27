package org.example.artyom.magicMechanism.network;

import org.bukkit.Location;

import java.util.*;

public class EnergyNetwork {
    private final Set<Location> cables = new HashSet<>();
    private final Set<Location> generators = new HashSet<>();
    private final Set<Location> consumers = new HashSet<>();
    private final Map<Location, Set<Location>> connections = new HashMap<>();
    private UUID networkId;
    private long lastValidated;

    public EnergyNetwork(UUID networkId) {
        this.networkId = networkId;
        this.lastValidated = System.currentTimeMillis();
    }

    public void addCable(Location loc) {
        cables.add(loc);
    }

    public void addGenerator(Location loc) {
        generators.add(loc);
    }

    public void addConsumer(Location loc) {
        consumers.add(loc);
    }

    public void addConnection(Location from, Location to) {
        connections.computeIfAbsent(from, k -> new HashSet<>()).add(to);
        connections.computeIfAbsent(to, k -> new HashSet<>()).add(from);
    }

    public boolean isValid() {
        // Сеть валидна если:
        // 1. Есть хотя бы один генератор
        // 2. Есть хотя бы один потребитель
        // 3. Все компоненты связаны в одну сеть
        return !generators.isEmpty() && !consumers.isEmpty() && isFullyConnected();
    }

    private boolean isFullyConnected() {
        if (cables.isEmpty() && generators.isEmpty() && consumers.isEmpty()) {
            return false;
        }

        // Выбираем стартовую точку (первый генератор или кабель)
        Location start = generators.isEmpty() ?
                cables.iterator().next() :
                generators.iterator().next();

        Set<Location> visited = new HashSet<>();
        dfs(start, visited);

        // Проверяем, что все компоненты сети посещены
        Set<Location> allComponents = new HashSet<>();
        allComponents.addAll(cables);
        allComponents.addAll(generators);
        allComponents.addAll(consumers);

        return visited.containsAll(allComponents);
    }

    private void dfs(Location current, Set<Location> visited) {
        if (visited.contains(current)) return;
        visited.add(current);

        Set<Location> neighbors = connections.getOrDefault(current, new HashSet<>());
        for (Location neighbor : neighbors) {
            dfs(neighbor, visited);
        }
    }

    public boolean canTransfer(Location generator, Location consumer) {
        if (!generators.contains(generator) || !consumers.contains(consumer)) {
            return false;
        }

        // Проверяем существует ли путь от генератора к потребителю
        Set<Location> visited = new HashSet<>();
        return hasPath(generator, consumer, visited);
    }

    private boolean hasPath(Location current, Location target, Set<Location> visited) {
        if (current.equals(target)) return true;
        if (visited.contains(current)) return false;

        visited.add(current);

        Set<Location> neighbors = connections.getOrDefault(current, new HashSet<>());
        for (Location neighbor : neighbors) {
            if (hasPath(neighbor, target, visited)) {
                return true;
            }
        }

        return false;
    }

    // Геттеры
    public Set<Location> getCables() { return Collections.unmodifiableSet(cables); }
    public Set<Location> getGenerators() { return Collections.unmodifiableSet(generators); }
    public Set<Location> getConsumers() { return Collections.unmodifiableSet(consumers); }
    public UUID getNetworkId() { return networkId; }
    public long getLastValidated() { return lastValidated; }
}
