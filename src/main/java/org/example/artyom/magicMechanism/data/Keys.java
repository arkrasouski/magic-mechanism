package org.example.artyom.magicMechanism.data;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import javax.naming.Name;

public final class Keys {
    private Keys() {}

    public static NamespacedKey CELL;
    public static NamespacedKey KEY_ITEMS;
    public static NamespacedKey MACHINE_TYPE, BUFFER, FREQ, CAPACITY;
    public static NamespacedKey INVENTORY_ITEM;

    public static NamespacedKey BARRIER_INV_MAIN;
    public static NamespacedKey[] BARRIER_INV_EDIT_PLAYER = new NamespacedKey[12];
    public static NamespacedKey[] BARRIER_ADDED_PLAYER_NAMES = new NamespacedKey[12];

    public static void init(JavaPlugin plugin) {
        CELL = new NamespacedKey(plugin, "energy_cell");
        MACHINE_TYPE = new NamespacedKey(plugin, "machine_type");

        BUFFER = new NamespacedKey(plugin, "buffer"); //current energy
        FREQ = new NamespacedKey(plugin, "freq_out");
        CAPACITY = new NamespacedKey(plugin, "capacity");

        KEY_ITEMS = new NamespacedKey(plugin, "gen_items");
        INVENTORY_ITEM = new NamespacedKey(plugin, "inventory_item");

        BARRIER_INV_MAIN = new NamespacedKey(plugin, "barrier_inv_main");

        for(int i = 0; i < BARRIER_INV_EDIT_PLAYER.length; i++) {
            BARRIER_INV_EDIT_PLAYER[i] = new NamespacedKey(plugin, "BARRIER_MAIN_MENU_EDIT_PLAYER_" + i);
        }
        for(int i = 0; i < BARRIER_ADDED_PLAYER_NAMES.length; i++) {
            BARRIER_ADDED_PLAYER_NAMES[i] = new NamespacedKey(plugin, "BARRIER_ADDED_PLAYER_NAMES_" + i);
        }
    }
}
