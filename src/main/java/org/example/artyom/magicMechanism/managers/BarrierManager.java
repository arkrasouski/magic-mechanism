package org.example.artyom.magicMechanism.managers;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.data.enums.MechanismType;
import org.example.artyom.magicMechanism.data.records.MechanismData;
import org.example.artyom.magicMechanism.mechanisms.Barrier;
import org.example.artyom.magicMechanism.mechanisms.BaseMechanism;
import org.example.artyom.magicMechanism.mechanisms.Generator;

import java.util.UUID;

public class BarrierManager extends BaseManager<Barrier> {


    public BarrierManager(MagicMechanism plugin) {
        super(plugin, MechanismType.BARRIER);
        loadAllMechanismsFromLoadedChunks();
    }

    @Override
    protected Barrier createMechanismInstance(Location location, Player owner, int energy, int capacity, boolean active) {
        return new Barrier(location, owner, energy, capacity, active);
    }


    @Override
    protected Barrier deserializeMechanism(MechanismData data, World world) {
        Location loc = data.toLocation(world);
        Player owner = data.owner() != null ?
                plugin.getServer().getPlayer(data.owner()) : null;

        return new Barrier(loc, owner, data.energy(),
                data.maxEnergy(), data.active());
    }
}
