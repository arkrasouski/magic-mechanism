package org.example.artyom.magicMechanism.events;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Panda;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.Keys;
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.service.GeneratorService;
import org.example.artyom.magicMechanism.utils.BaseMechanismUtil;
import org.example.artyom.magicMechanism.utils.EnergyCellUtil;
import org.example.artyom.magicMechanism.utils.GeneratorUtil;
import org.example.artyom.magicMechanism.utils.ToolUtil;

public class GeneratorEvents extends BaseMechanismEvents {


    public GeneratorEvents() {
        super(new GeneratorUtil());
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        Inventory inv = e.getInventory();
        InventoryHolder holder = inv.getHolder();
        if (!(holder instanceof TileState tile)) return;
        if (tile.getType() != Material.DROPPER) return;

        String type = this.mechanism.getMechanismType(tile);
        if (!this.mechanism.getKey_type().equals(type)) return;

        long buf = tile.getPersistentDataContainer().getOrDefault(Keys.BUFFER, PersistentDataType.INTEGER, 0);
        int freq = tile.getPersistentDataContainer().getOrDefault(Keys.FREQ, PersistentDataType.INTEGER, this.mechanism.getFrequency());

        if (e.getPlayer() instanceof Player p) {
            p.sendMessage(ChatColor.YELLOW + this.mechanism.getName() + ": энергия=" + buf + "/" + this.mechanism.getCapacity()
                    + " частота=" + freq);
        }
    }
    @EventHandler
    public void onClickEnergySlot(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof TileState tile)) return;
        if (tile.getType() != this.mechanism.getMaterial()) return;

        String type = this.mechanism.getMechanismType(tile);
        if (!this.mechanism.getKey_type().equals(type)) return;

        // Клик по верхнему инвентарю (сам дроппер)
        if (e.getClickedInventory() == null) return;
        if (!e.getClickedInventory().equals(top)) return;

        if (e.getSlot() != 0) return; // первый слот дроппера
        //System.out.println("Dropper");
        // ВАЖНО: во время InventoryClickEvent инвентарь ещё "старый".
        // Поэтому проверяем результат на следующий тик.
        Bukkit.getScheduler().runTask(MagicMechanism.getInstance(), () -> {
            ItemStack inSlot = top.getItem(0);
            if (EnergyCellUtil.isEnergyCell(inSlot)) {
                //System.out.println("isEnergyCell");
                Player p = (Player) e.getWhoClicked();
                p.sendMessage("Аккумулятор вставлен в слот 1!");
                // тут можешь запускать логику зарядки/линка
                GeneratorService.onCellInserted(tile.getLocation());
            }
            else {
                GeneratorService.onCellRemoved(tile.getLocation());
            }
        });
    }
}
