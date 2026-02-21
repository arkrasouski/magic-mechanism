package org.example.artyom.magicMechanism.inventories;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.example.artyom.magicMechanism.utils.LogUtil;

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


    public Inventory openMenu(Player p, MechanismHolder holder, double percent) {
        Inventory inv = Bukkit.createInventory(holder, this.size, this.glif);
        updateEnergyBar(inv, holder, percent);
        return inv;
    }


    public static void updateEnergyBar(Inventory top, MechanismHolder h, double percent) {
        int size = top.getSize(); // ✅ ТОЧНЫЙ размер целевого инвентаря
        int start = size - 9, end = size - 1;
        int segments = end - start + 1;
        int filled = (int) Math.round(clamp(percent, 0, 100) / 100.0 * segments);

        LogUtil.warn("EnergyBar | size=" + size + " | segments=" + segments + " | filled=" + filled);

        for (int i = 0; i < segments; i++) {
            int slot = start + i;
            if (i >= filled) {
                top.setItem(slot, null);
                continue;
            }

            String id = i == 0 ? "mehanisms:transformer_energy_left" :
                    i == segments - 1 ? "mehanisms:transformer_energy_right" :
                            "mehanisms:transformer_energy_middle";

            CustomStack cs = CustomStack.getInstance(id);
            top.setItem(slot, cs != null ? cs.getItemStack() : null);
        }

        h.setNewEnergy(percent);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    public int findTargetSlot(Inventory top) {

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
