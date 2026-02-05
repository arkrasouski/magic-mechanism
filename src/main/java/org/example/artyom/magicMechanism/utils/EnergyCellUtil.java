package org.example.artyom.magicMechanism.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.example.artyom.magicMechanism.Keys;
import org.example.artyom.magicMechanism.MagicMechanism;

import java.util.Arrays;

public class EnergyCellUtil {

    public static ItemStack makeEnergyCell(int capacity, int energy, int freq) {
        ItemStack cell = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = cell.getItemMeta();

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(Keys.CELL, PersistentDataType.STRING, "EnergyCell");
        pdc.set(Keys.CELL_CAP, PersistentDataType.INTEGER, capacity);
        int current_energy = Math.min(energy, capacity);
        pdc.set(Keys.CELL_ENERGY,  PersistentDataType.INTEGER, current_energy );
        meta.setLore(Arrays.asList("energy: " + current_energy + "/" + capacity));

        pdc.set(Keys.FREQ,  PersistentDataType.INTEGER, freq);

        meta.setDisplayName("Энерго-блок");
        cell.setItemMeta(meta);
        return cell;
    }

    public static boolean isEnergyCell(ItemStack item){
        if (item == null) return false;

        if(item.getType() != Material.NETHER_STAR) return false;
        // Проверяем базовый тип блока

        ItemMeta meta = item.getItemMeta();

        // Проверяем, что блок поддерживает PDC
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // Проверяем наш ключ
        String type = pdc.get(Keys.CELL , PersistentDataType.STRING);

        return "EnergyCell".equals(type);
    }


    public static int getEnergy(ItemStack cell) {
        ItemMeta meta = cell.getItemMeta();
        if (meta == null) return 0;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.getOrDefault(Keys.CELL_ENERGY, PersistentDataType.INTEGER, 0); // getOrDefault [web:19]
    }

    public static void setEnergy(ItemStack cell, int newEnergy) {
        if (cell == null || cell.getType().isAir()) return;

        ItemMeta meta = cell.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int cap = pdc.getOrDefault(Keys.CELL_CAP, PersistentDataType.INTEGER, 0);

        int clamped = Math.max(0, Math.min(newEnergy, cap));
        pdc.set(Keys.CELL_ENERGY, PersistentDataType.INTEGER, clamped); // set [web:19]
        meta.setLore(Arrays.asList("energy: " + clamped + "/" + cap));
        cell.setItemMeta(meta);
    }
}
