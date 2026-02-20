package org.example.artyom.magicMechanism.inventories.barrier;

import org.bukkit.Location;
import org.example.artyom.magicMechanism.data.enums.MechanismType;
import org.example.artyom.magicMechanism.data.enums.ScreenCategory;
import org.example.artyom.magicMechanism.inventories.MechanismHolder;

public class BarrierHolder extends MechanismHolder {

        ScreenCategory screen;

    public BarrierHolder(Location location, MechanismType mechanism,
                         double energyPercent, ScreenCategory screen) {
        super(location, mechanism, energyPercent);
        this.screen = screen;
    }

    @Override
    public ScreenCategory getScreenCategory() {
        return screen.getCategory();
    }

}
