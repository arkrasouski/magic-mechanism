package org.example.artyom.magicMechanism.inventories;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;
import dev.lone.itemsadder.api.CustomStack;
public class FillGenInventory {

    public static Inventory  openMenu(Player p, GenHolder holder, double percent) {
        Inventory inv = Bukkit.createInventory(holder, 27, "&f:offset_-64::transformer_menu::offset_64:");
        updateEnergyBar(inv, percent);
        return inv;
    }


    public static void updateEnergyBar(Inventory top, double percent) {
        int start = 18, end = 26;
        int segments = end - start + 1;
        int filled = (int) Math.round(clamp(percent, 0, 100) / 100.0 * segments);

        for (int i = 0; i < segments; i++) {
            int slot = start + i;

            if (i >= filled) {
                top.setItem(slot, null); // или new ItemStack(Material.AIR)
                continue;
            }

            String id;
            if (i == 0) id = "mehanisms:transformer_energy_left";
            else if (i == segments - 1) id = "mehanisms:transformer_energy_right";
            else id = "mehanisms:transformer_energy_middle";

            CustomStack cs = CustomStack.getInstance(id);
            if (cs == null) {
                top.setItem(slot, null); // если IA item не найден — тоже чистим, чтобы не висел старый
                continue;
            }

            top.setItem(slot, cs.getItemStack());
        }
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
