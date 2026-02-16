package org.example.artyom.magicMechanism.inventories;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseFillCustomInventory {
    public int getSize() {
        return size;
    }

    private final int size;
    private final String glif;
    private final List<Integer> activeSlots;

    protected BaseFillCustomInventory(int size, String glif, List<Integer> activeSlots) {
        this.size = size; //27
        this.glif = glif; //"&f:offset_-64::transformer_menu::offset_64:"
        this.activeSlots = activeSlots; //[9, 10]
    }


    public Inventory openMenu(Player p, GenHolder holder, double percent) {
        Inventory inv = Bukkit.createInventory(holder, this.size, this.glif);
        updateEnergyBar(inv, percent);
        return inv;
    }


    public void updateEnergyBar(Inventory top, double percent) {
        int start = this.size - 9, end = this.size - 1;
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

    public int findTargetSlot(Inventory top) {
        // Пример: разрешены только 19-27 и только если слот пустой
        if(activeSlots ==null) return -1;
        for (int slot : activeSlots) {
            ItemStack cur = top.getItem(slot);
            if (cur == null || cur.getType().isAir()) return slot;
        }
        return -1;
    }
    public boolean isBlocked(int slot) {
        // пример: заблокировать ВСЕ слоты верхнего инвентаря
        // return true;
        if(activeSlots ==null) return true;
        for (int i : activeSlots) {
            if (slot == i) return false;
        }
        return true;

    }

}
