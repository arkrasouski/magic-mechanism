package org.example.artyom.magicMechanism.inventories;

import org.bukkit.Location;
import org.example.artyom.magicMechanism.data.enums.MechanismType;
import org.example.artyom.magicMechanism.data.enums.ScreenCategory;

public class EditPlayerHolder extends MechanismHolder{
    ScreenCategory screen;

    public EditPlayerHolder(Location location, MechanismType mechanism,
                         double energyPercent, ScreenCategory screen) {
        super(location, mechanism, energyPercent);
        this.screen = screen;
    }

    @Override
    public ScreenCategory getScreenCategory() {
        return screen.getCategory();
    }
}
