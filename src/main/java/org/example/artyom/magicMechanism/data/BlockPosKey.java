package org.example.artyom.magicMechanism.data;

import org.bukkit.Location;

import java.util.UUID;

public record BlockPosKey(UUID worldId, int x, int y, int z) {
    public static BlockPosKey of(Location loc) {
        return new BlockPosKey(
                loc.getWorld().getUID(),
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ()
        );
    }
}
