package org.example.artyom.magicMechanism.inventories;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class GenHolder implements InventoryHolder {

    private final Location location;   // где стоит генератор/аккумулятор
    private Inventory inventory;        // сюда положим созданный GUI

    public GenHolder(Location location) {
        this.location = location.clone(); // чтобы не зависеть от внешних мутаций
    }

    public Location getLocation() {
        return location.clone();
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}

