package org.example.artyom.magicMechanism.data.records;

import org.example.artyom.magicMechanism.utils.LogUtil;

import java.util.UUID;

public record GeneratorData(int x, int y, int z, int energy, int maxEnergy, boolean active, UUID owner) {
    public String serialize() {
        return String.format("%d;%d;%d;%d;%d;%b;%s",
                x, y, z, energy, maxEnergy, active, owner != null ? owner.toString() : "null");
    }

    public static GeneratorData deserialize(String data) {
        String[] parts = data.split(";");
        if (parts.length < 6) return null;

        try {
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            int energy = Integer.parseInt(parts[3]);
            int maxEnergy = Integer.parseInt(parts[4]);
            boolean active = Boolean.parseBoolean(parts[5]);
            UUID owner = parts.length > 6 && !parts[6].equals("null") ?
                    UUID.fromString(parts[6]) : null;

            return new GeneratorData(x, y, z, energy, maxEnergy, active, owner);
        } catch (Exception e) {
            LogUtil.warn("Ошибка десериализации генератора: " + data);
            return null;
        }
    }
}