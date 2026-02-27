package org.example.artyom.magicMechanism.managers;


import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.data.enums.MechanismType;
import org.example.artyom.magicMechanism.data.records.MechanismData;
import org.example.artyom.magicMechanism.mechanisms.Barrier;


import java.util.UUID;

public class BarrierManager extends BaseManager<Barrier> {


    public BarrierManager(MagicMechanism plugin) {
        super(plugin, MechanismType.BARRIER);
        loadAllMechanismsFromLoadedChunks();
    }

    @Override
    protected Barrier createMechanismInstance(Location location, Player owner, int energy, int capacity, boolean active) {
        return Barrier.create(location, owner.getUniqueId(), this);
    }


    @Override
    protected Barrier deserializeMechanism(MechanismData data, World world) {
        Location loc = data.toLocation(world);
        UUID owner = data.owner() != null ?
                data.owner() : null;

        return Barrier.create(loc, owner, this);
    }
}
