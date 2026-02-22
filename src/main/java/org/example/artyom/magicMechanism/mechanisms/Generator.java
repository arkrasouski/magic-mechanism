package org.example.artyom.magicMechanism.mechanisms;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.example.artyom.magicMechanism.data.enums.MechanismType;



public class Generator extends BaseMechanism {

    public static final int capacity = 1000;
    public static final int frequency = 10;
    public static final int frequencySpeed = 20;
    public Generator(Location location, Player owner) {
        super(location,
                MechanismType.GENERATOR,
                owner, false,
                0, capacity,
                frequency, frequencySpeed);
    }

    public Generator(Location location, Player owner, int energy, int capacity, boolean isActive) {
          super(location,
                MechanismType.GENERATOR,
                owner, isActive,
                energy, capacity,
                frequency, frequencySpeed);
    }


    static final BlockFace[] FACES_6 = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
    };


}
