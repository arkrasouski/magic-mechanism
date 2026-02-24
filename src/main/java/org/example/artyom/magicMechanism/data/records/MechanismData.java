package org.example.artyom.magicMechanism.data.records;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Данные механизма для сериализации
 */
public record MechanismData(
        int x,
        int y,
        int z,
        int energy,
        int maxEnergy,
        boolean active,
        UUID owner
) {

    public String serialize() {
        return String.format("%d;%d;%d;%d;%d;%b;%s",
                x, y, z, energy, maxEnergy, active,
                owner != null ? owner.toString() : "null");
    }

    public static MechanismData deserialize(String data) {
        try {
            String[] parts = data.split(";");
            if (parts.length < 7) return null;

            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            int energy = Integer.parseInt(parts[3]);
            int maxEnergy = Integer.parseInt(parts[4]);
            boolean active = Boolean.parseBoolean(parts[5]);

            UUID owner = null;
            if (!parts[6].equals("null")) {
                owner = UUID.fromString(parts[6]);
            }

            return new MechanismData(x, y, z, energy, maxEnergy, active, owner);
        } catch (Exception e) {
            return null;
        }
    }

    public Location toLocation(World world) {
        return new Location(world, x, y, z);
    }
}
