package org.example.artyom.magicMechanism.data.enums;

import org.bukkit.Material;

public enum MechanismCategory {
    GENERATOR(true, true),
    CONSUMER(true, true),
    CABLE(true, false),
    STORAGE(true, true),
    ISOLATOR(false, false);

    private final boolean canConnect;
    private final boolean canStoreEnergy;

    MechanismCategory(boolean canConnect, boolean canStoreEnergy) {
        this.canConnect = canConnect;
        this.canStoreEnergy = canStoreEnergy;
    }

    public boolean canConnect() { return canConnect; }
    public boolean canStoreEnergy() { return canStoreEnergy; }
}

