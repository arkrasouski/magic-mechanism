package org.example.artyom.magicMechanism.energyitems;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.data.Keys;

import java.util.Arrays;

public abstract class BaseEnergyItem {

    private Material material;

    protected String key_type = "energy_cell";

    private int capacity;
    private int frequency;

    private String name;
    private String lore;

    public BaseEnergyItem(Material material, String key_type, int capacity, int frequency, String name, String lore) {
        this.material = material;
        this.key_type = key_type;
        this.capacity = capacity;
        this.frequency = frequency;
        this.name = name;
        this.lore = lore;
    }

    public ItemStack makeEnergyItem(int energy) {
        ItemStack cell = new ItemStack(this.material); //Material.NETHER_STAR
        ItemMeta meta = cell.getItemMeta();

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(Keys.CELL, PersistentDataType.STRING, this.key_type);
        pdc.set(Keys.CAPACITY, PersistentDataType.INTEGER, this.capacity);
        int current_energy = Math.min(energy, this.capacity);
        pdc.set(Keys.BUFFER,  PersistentDataType.INTEGER, current_energy );
        meta.setLore(Arrays.asList("energy: " + current_energy + "/" + this.capacity));

        pdc.set(Keys.FREQ,  PersistentDataType.INTEGER, this.frequency);

        meta.setDisplayName(this.name);
        cell.setItemMeta(meta);
        return cell;
    }



    public static int getEnergy(ItemStack cell) {
        if (cell == null || cell.getType() == Material.AIR) return 0;
        ItemMeta meta = cell.getItemMeta();
        if (meta == null) return 0;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.getOrDefault(Keys.BUFFER, PersistentDataType.INTEGER, 0);
    }

    public static void setEnergy(ItemStack cell, int newEnergy) {
        if (cell == null || cell.getType().isAir()) return;

        ItemMeta meta = cell.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int cap = pdc.getOrDefault(Keys.CAPACITY, PersistentDataType.INTEGER, 0);

        int clamped = Math.max(0, Math.min(newEnergy, cap));
        pdc.set(Keys.BUFFER, PersistentDataType.INTEGER, clamped); // set [web:19]
        meta.setLore(Arrays.asList("energy: " + clamped + "/" + cap));
        cell.setItemMeta(meta);
    }
}
