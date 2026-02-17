package org.example.artyom.magicMechanism.inventories;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class BarrierInventory extends BaseFillCustomInventory{

    public BarrierInventory() {
        super(36, "&f:offset_-64::barrier_menu::offset_64:", null);
    }

    @Override
    public Inventory openMenu(Player p, MechanismHolder holder, double percent) {
        Inventory inv = super.openMenu(p, holder, percent);
        //inv.setItem();
        return inv;
    }
}
