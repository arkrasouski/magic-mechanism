package org.example.artyom.magicMechanism.inventories;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.example.artyom.magicMechanism.data.enums.MechanismType;
import org.jetbrains.annotations.NotNull;

public final class MechanismHolder implements InventoryHolder {

    private final Location location;   // где стоит генератор/аккумулятор
    private Inventory inventory;        // сюда положим созданный GUI
    private final MechanismType type;
    private double energyPercent;

    public MechanismHolder(Location location, MechanismType type, double energyPercent) {
        this.location = location.clone();
        this.type = type;
        this.energyPercent = energyPercent;
    }

    public Location getLocation() { return location.clone(); }
    public MechanismType getType() { return type; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    public void setNewEnergy(double percent) {this.energyPercent = percent;}
    public double getEnergyPercent(){return this.energyPercent;}
    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}

