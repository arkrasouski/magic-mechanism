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

    private final Location location;
    private MechanismType mechanismType;
    private Player owner;
    private boolean isActive;
    private int energyLevel;
    private int capacity;
    private int frequency;
    private int freqSpeed;

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
        pdc.set(Keys.BUFFER,  PersistentDataType.INTEGER, 0);
        pdc.set(Keys.CAPACITY, PersistentDataType.INTEGER, this.capacity);
        pdc.set(Keys.FREQ, PersistentDataType.INTEGER, this.frequency);
        tile.update();

    }

    public boolean isMechanismBlock(Block block){
        if (block == null) return false;

        // Проверяем базовый тип блока
        if (block.getType() != this.mechanismType.getMaterial()) return false;

        BlockState state = block.getState();

        // Проверяем, что блок поддерживает PDC
        if (!(state instanceof TileState tile)) return false;

        PersistentDataContainer pdc = tile.getPersistentDataContainer();

        // Проверяем наш ключ
        String type = pdc.get(Keys.MACHINE_TYPE , PersistentDataType.STRING);

        return this.mechanismType.name().equals(type);
    }

    //Getter and setter
    public Location getLocation() { return location; }
    public int getEnergyLevel() { return energyLevel; }
    public void setEnergyLevel(int energyLevel) { this.energyLevel = energyLevel; }
    public int getCapacity() {return capacity;}
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public Player getOwner() { return owner; }
    public MechanismType getMechanismType() {return this.mechanismType;}
    public int getFrequency() {return frequency;}
    public int getFreqSpeed() {return freqSpeed;}

}
