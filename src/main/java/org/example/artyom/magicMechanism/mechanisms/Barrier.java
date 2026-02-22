package org.example.artyom.magicMechanism.mechanisms;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.example.artyom.magicMechanism.data.enums.MechanismType;

public class Barrier extends BaseMechanism {
    public static final int capacity = 750;
    public static int frequency = 100;
    public static int frequencySpeed = 60*60*20;
    public Barrier(Location location, Player owner) {
        super(location,
                MechanismType.BARRIER,
                owner, false,
                0, capacity,
                frequency, frequencySpeed);
    }
    public Barrier(Location location, Player owner, int energy, int capacity, boolean isActive) {
          super(location,
                MechanismType.BARRIER,
                owner, isActive,
                energy, capacity,
                frequency, frequencySpeed);
    }

}