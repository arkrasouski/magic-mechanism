package org.example.artyom.magicMechanism.inventories;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.example.artyom.magicMechanism.data.enums.MechanismType;
import org.example.artyom.magicMechanism.data.enums.Screen;
import org.example.artyom.magicMechanism.data.enums.ScreenCategory;
import org.jetbrains.annotations.NotNull;

public abstract class MechanismHolder implements InventoryHolder {

    private final Location location;   // где стоит генератор/аккумулятор
    private Inventory inventory;        // сюда положим созданный GUI
    private final MechanismType mechanism;
    private double energyPercent;

    public MechanismHolder(Location location, MechanismType mechanism, double energyPercent) {
        this.location = location.clone();
        this.mechanism = mechanism;
        this.energyPercent = energyPercent;

    }

    public Location getLocation() { return location.clone(); }
    public MechanismType getType() { return mechanism; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    public void setNewEnergy(double percent) {this.energyPercent = percent;}
    public double getEnergyPercent(){return this.energyPercent;}
    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
    public abstract ScreenCategory getScreenCategory();
}

