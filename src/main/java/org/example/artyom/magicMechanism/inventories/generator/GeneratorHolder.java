package org.example.artyom.magicMechanism.inventories.generator;

import org.bukkit.Location;
import org.example.artyom.magicMechanism.data.enums.MechanismType;
import org.example.artyom.magicMechanism.data.enums.ScreenCategory;
import org.example.artyom.magicMechanism.inventories.MechanismHolder;

public class GeneratorHolder extends MechanismHolder {
    public GeneratorHolder(Location location, MechanismType mechanism,
                         double energyPercent) {
        super(location, mechanism, energyPercent);
    }

    @Override
    public ScreenCategory getScreenCategory() {
        return null;
    }
}
