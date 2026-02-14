package org.example.artyom.magicMechanism.events;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Panda;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.Keys;
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.inventories.GenHolder;
import org.example.artyom.magicMechanism.inventories.GenStorage;
import org.example.artyom.magicMechanism.service.GeneratorService;
import org.example.artyom.magicMechanism.utils.BaseMechanismUtil;
import org.example.artyom.magicMechanism.utils.EnergyCellUtil;
import org.example.artyom.magicMechanism.utils.GeneratorUtil;
import org.example.artyom.magicMechanism.utils.ToolUtil;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GeneratorEvents extends BaseMechanismEvents {



    public GeneratorEvents() {
        super(new GeneratorUtil());
    }

@EventHandler
public void onInteract(PlayerInteractEvent e) {
    if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    Block b = e.getClickedBlock();
    if (b == null) return;

    // генератор — это, например, DROPPER
    if (!GeneratorUtil.isGenerator(b)) return;

    TileState tile = (TileState) b.getState();



    e.setCancelled(true); // чтобы не открылся ванильный дроппер

    GenHolder holder = new GenHolder(b.getLocation());
    Inventory gui = Bukkit.createInventory(holder, 27, this.mechanism.getName());
    holder.setInventory(gui);

    // если тебе надо переносить реальные предметы из дроппера в GUI — решай сам:
    // либо gui = tile.getInventory(), либо loadItems(tile, gui) из PDC-хранилища.
    GenStorage.loadItems(tile, gui);


    int buf = tile.getPersistentDataContainer().getOrDefault(Keys.BUFFER, PersistentDataType.INTEGER, 0);
    int freq = tile.getPersistentDataContainer().getOrDefault(Keys.FREQ, PersistentDataType.INTEGER, this.mechanism.getFrequency());

    if (e.getPlayer() instanceof Player p) {
        p.sendMessage(ChatColor.YELLOW + this.mechanism.getName() + ": энергия=" + buf + "/" + this.mechanism.getCapacity()
                + " частота=" + freq);
        p.openInventory(gui);
    }
}
    @EventHandler
    public void onClickEnergySlot(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof GenHolder h)) return;

        Bukkit.getScheduler().runTask(MagicMechanism.getInstance(), () -> {
            ItemStack cell = top.getItem(0);
            Location loc = h.getLocation();

            BlockState st = loc.getBlock().getState();
            if (!(st instanceof TileState tile)) return;
            Player p = (Player) e.getWhoClicked();

            if (EnergyCellUtil.isEnergyCell(cell)) {
                p.sendMessage("Аккумулятор вставлен в слот 1!");
                GeneratorService.onCellInserted(h.getLocation());
            } else {
                GeneratorService.onCellRemoved(h.getLocation());
            }
            // КЛЮЧЕВОЕ: синхронизируем PDC сразу
            GenStorage.saveItems(tile, top);
        });
    }
    @EventHandler
    public void onGuiDrag(InventoryDragEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof GenHolder h)) return;

        int slot0Raw = 0; // в topInventory raw слоты начинаются с 0
        if (!e.getRawSlots().contains(slot0Raw)) return;

        Bukkit.getScheduler().runTask(MagicMechanism.getInstance(), () -> {
            ItemStack cell = top.getItem(0);
            Location loc = h.getLocation();

            BlockState st = loc.getBlock().getState();
            if (!(st instanceof TileState tile)) return;
            Player p = (Player) e.getWhoClicked();

            if (EnergyCellUtil.isEnergyCell(cell)) {
                p.sendMessage("Аккумулятор вставлен в слот 1!");
                GeneratorService.onCellInserted(h.getLocation());
            } else {
                GeneratorService.onCellRemoved(h.getLocation());
            }
            // КЛЮЧЕВОЕ: синхронизируем PDC сразу
            GenStorage.saveItems(tile, top);
        });
    }
}
