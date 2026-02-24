package org.example.artyom.magicMechanism.mechanisms;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.data.Keys;
import org.example.artyom.magicMechanism.data.enums.MechanismType;

public abstract class BaseMechanism {

    protected final Location location;
    protected static MechanismType mechanismType;
    protected  Player owner;
    protected  boolean isActive;
    protected  int energyLevel;
    protected  int capacity;
    protected  int frequency;
    protected  int freqSpeed;

    public BaseMechanism(Location location,
                         MechanismType mechanismType, Player owner, boolean isActive, int energyLevel,
                         int capacity, int frequency, int freqSpeed) {
        this.location = location;
        this.mechanismType = mechanismType;
        this.owner = owner;
        this.isActive = isActive;
        this.energyLevel = energyLevel;
        this.capacity = capacity;
        this.frequency = frequency;
        this.freqSpeed = freqSpeed;
    }



    public void setMechanismBlock(Block block) {
        if (block == null) return;

        // Убеждаемся, что это нужный базовый блок
        if (block.getType() != this.mechanismType.getMaterial()) return; {
            block.setType(this.mechanismType.getMaterial());
        }

        BlockState state = block.getState();
        if (!(state instanceof TileState tile)) return;
        PersistentDataContainer pdc = tile.getPersistentDataContainer();

        // Помечаем блок как наш механизм
        pdc.set(
                Keys.MACHINE_TYPE,
                PersistentDataType.STRING,
                this.mechanismType.name()
        );
        tile.update();

    }

    //Getter and setter
    public Location getLocation() { return location; }
    public int getEnergyLevel() { return energyLevel; }
    public void setEnergyLevel(int energyLevel) { this.energyLevel = energyLevel; }
    public int getCapacity() {return capacity;}
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public Player getOwner() { return owner; }
    public MechanismType getMechanismType() {return mechanismType;};
    public int getFrequency() {return frequency;}
    public int getFreqSpeed() {return freqSpeed;}

}
