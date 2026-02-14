package org.example.artyom.magicMechanism;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.units.qual.N;

import javax.naming.Name;

public final class Keys {
    private Keys() {}

    public static NamespacedKey CELL;
    public static NamespacedKey KEY_ITEMS;
    public static NamespacedKey MACHINE_TYPE, BUFFER, FREQ, CAPACITY;

    public static void init(JavaPlugin plugin) {
        CELL = new NamespacedKey(plugin, "energy_cell");
        MACHINE_TYPE = new NamespacedKey(plugin, "machine_type");

        BUFFER = new NamespacedKey(plugin, "buffer"); //current energy
        FREQ = new NamespacedKey(plugin, "freq_out");
        CAPACITY = new NamespacedKey(plugin, "capacity");

        KEY_ITEMS = new NamespacedKey(plugin, "gen_items");

    }
}
