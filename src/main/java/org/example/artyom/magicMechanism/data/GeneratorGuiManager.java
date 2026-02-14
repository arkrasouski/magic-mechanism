package org.example.artyom.magicMechanism.data;

import org.bukkit.Location;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GeneratorGuiManager{

    private final Map<BlockPosKey, Set<UUID>> viewers = new ConcurrentHashMap<>();

    public void addViewer(Location loc, UUID playerId) {
        viewers.computeIfAbsent(BlockPosKey.of(loc), __ -> ConcurrentHashMap.newKeySet()).add(playerId);
    }

    public void removeViewer(Location loc, UUID playerId) {
        BlockPosKey key = BlockPosKey.of(loc);
        Set<UUID> set = viewers.get(key);
        if (set == null) return;
        set.remove(playerId);
        if (set.isEmpty()) viewers.remove(key);
    }

    public Map<BlockPosKey, Set<UUID>> viewers() {
        return viewers;
    }
    public boolean hasViewers() {
        return !viewers.isEmpty();
    }
}
