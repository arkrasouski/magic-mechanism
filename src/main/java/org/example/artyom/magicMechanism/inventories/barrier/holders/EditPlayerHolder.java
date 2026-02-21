package org.example.artyom.magicMechanism.inventories.barrier.holders;

import org.bukkit.Location;
import org.example.artyom.magicMechanism.data.enums.MechanismType;
import org.example.artyom.magicMechanism.data.enums.ScreenCategory;

public class EditPlayerHolder extends BarrierHolder {
    private int slot = -1;
    public EditPlayerHolder(Location location, MechanismType mechanism,
                         double energyPercent, ScreenCategory screen) {
        super(location, mechanism, energyPercent, screen);
    }

    public EditPlayerHolder(Location location, MechanismType mechanism,
                            double energyPercent, ScreenCategory screen, int slot) {
        super(location, mechanism, energyPercent, screen);
        this.slot = slot;
    }

    public int getSlot() {
        return slot;
    }
}
