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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
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
        InventoryHolder h = top.getHolder();
        if (!(h instanceof GenHolder holder)) return;

        // работаем только с кликами по верхнему (нашему) GUI
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(top)) return;

        if (e.getSlot() != 0) return; // слот 0 — под аккумулятор

        // ВАЖНО: если ты НЕ отменяешь событие, содержимое слота изменится после обработки,
        // поэтому проверяем на следующий тик (как ты и делал).
        Bukkit.getScheduler().runTask(MagicMechanism.getInstance(), () -> {
            ItemStack inSlot = top.getItem(0);

            Location loc = holder.getLocation();
            BlockState st = loc.getBlock().getState();
            if (!(st instanceof TileState tile)) return;

            String type = this.mechanism.getMechanismType(tile);
            if (!this.mechanism.getKey_type().equals(type)) return;

            Player p = (Player) e.getWhoClicked();

            if (EnergyCellUtil.isEnergyCell(inSlot)) {
                p.sendMessage("Аккумулятор вставлен в слот 1!");
                GeneratorService.onCellInserted(loc);
            } else {
                GeneratorService.onCellRemoved(loc);
            }

            // если ты сохраняешь GUI в PDC — тут можно дернуть saveItems(tile, top)
            // и не забыть tile.update() внутри saveItems [web:69]
        });
    }

}
