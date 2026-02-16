package org.example.artyom.magicMechanism.inventories;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import dev.lone.itemsadder.api.CustomStack;
public class FillGenInventory extends BaseFillCustomInventory {

    public FillGenInventory() {
        super(27, "&f:offset_-64::transformer_menu::offset_64:", Arrays.asList(9, 10));
    }

}
