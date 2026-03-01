package org.example.artyom.magicMechanism.mechanisms;

import org.bukkit.Location;
import org.bukkit.World;

import org.example.artyom.magicMechanism.data.enums.MechanismType;
import org.example.artyom.magicMechanism.data.records.MechanismData;

import java.util.UUID;

public abstract class BaseMechanism {

    protected Location location;
    protected MechanismType type;
    protected UUID owner;
    protected boolean active;
    protected int energyLevel;
    protected int capacity;


    public BaseMechanism(Location location, MechanismType type, UUID owner, int capacity) {
        this.location = location;
        this.type = type;
        this.owner = owner;
        this.active = true;
        this.energyLevel = 0;
        this.capacity = capacity;
    }

    /**
     * Конвертирует механизм в MechanismData для сохранения
     */
    public abstract MechanismData toData();

    /**
     * Создает механизм из MechanismData
     */
    public static BaseMechanism fromData(World world, MechanismData data, MechanismType type) {
        Location loc = new Location(world, data.x(), data.y(), data.z());

        return switch (type) {
            case GENERATOR -> new Generator(loc, data.owner(), data.energy(), data.maxEnergy());
            case BARRIER -> new Barrier(loc, data.owner(), data.energy(), data.maxEnergy());
            case CABLE -> new Cable(loc, data.owner(), data.energy(), data.maxEnergy());
            default -> null;
        };
    }

    // Геттеры и сеттеры
    public Location getLocation() { return location; }
    public MechanismType getType() { return type; }
    public UUID getOwner() { return owner; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    // Абстрактные методы для энергии
    public int getEnergyLevel(){return energyLevel;};
    public void setEnergyLevel(int energy){this.energyLevel = energy;};
    public int getCapacity(){return capacity;};
}