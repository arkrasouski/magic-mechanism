package org.example.artyom.magicMechanism;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class Keys {
    private Keys() {}

    public static NamespacedKey CELL, CELL_ENERGY, CELL_CAP;
    public static NamespacedKey MACHINE_TYPE, BUFFER, FREQ;

    public static void init(JavaPlugin plugin) {
        CELL = new NamespacedKey(plugin, "energy_cell");
        CELL_ENERGY = new NamespacedKey(plugin, "cell_energy");
        CELL_CAP = new NamespacedKey(plugin, "cell_capacity");

        MACHINE_TYPE = new NamespacedKey(plugin, "machine_type");
        BUFFER = new NamespacedKey(plugin, "buffer");
        FREQ = new NamespacedKey(plugin, "freq_out");
    }
}
