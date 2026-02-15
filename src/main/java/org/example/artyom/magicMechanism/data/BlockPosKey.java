package org.example.artyom.magicMechanism.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import javax.annotation.Nullable;
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
    public static BlockPosKey of(Block b) {
        return new BlockPosKey(b.getWorld().getUID(), b.getX(), b.getY(), b.getZ());
    }
    @Nullable
    public static Block blockFromKey(BlockPosKey key) {
        World world = Bukkit.getWorld(key.worldId()); // или key.world() [web:92]
        if (world == null) return null;                 // мир не загружен [web:92]
        return world.getBlockAt(key.x(), key.y(), key.z()); // [web:94]
    }
}
