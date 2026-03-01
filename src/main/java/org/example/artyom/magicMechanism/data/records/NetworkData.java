package org.example.artyom.magicMechanism.data.records;

import org.bukkit.Location;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record NetworkData(
        UUID networkId,
        Set<Location> cables,
        Set<Location> generators,
        Set<Location> consumers,
        Map<Location, Set<Location>> connections,
        long lastValidated,
        int totalEnergy,
        int maxCapacity
) {
    public boolean isEmpty() {
        return cables.isEmpty() && generators.isEmpty() && consumers.isEmpty();
    }

    public int getComponentCount() {
        return cables.size() + generators.size() + consumers.size();
    }
}
