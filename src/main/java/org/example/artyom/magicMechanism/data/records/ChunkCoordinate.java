package org.example.artyom.magicMechanism.data.records;


import org.bukkit.Chunk;
import org.bukkit.Location;

public record ChunkCoordinate(String world, int x, int z) {
    public static ChunkCoordinate of(Chunk chunk) {
        return new ChunkCoordinate(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    public static ChunkCoordinate of(Location location) {
        return new ChunkCoordinate(
                location.getWorld().getName(),
                location.getBlockX() >> 4,
                location.getBlockZ() >> 4
        );
    }
}
