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
import org.example.artyom.magicMechanism.utils.EnergyCellUtil;
import org.example.artyom.magicMechanism.utils.GeneratorUtil;
import org.example.artyom.magicMechanism.utils.ToolUtil;

public class GeneratorEvents implements Listener {
//    @EventHandler
//    public void onClick(PlayerInteractEvent e) {
//        if (e.getClickedBlock() == null) return;
//
//        Block block = e.getClickedBlock();
//        if (!GeneratorUtil.isGenerator(block)) return;
//
//        //e.setCancelled(true);
//        e.getPlayer().sendMessage("Энерго трансформатор активирован!");
//    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {

        ItemStack item = e.getItemInHand();
        if (!GeneratorUtil.isGeneratorItem(item)) return;

        Block block = e.getBlockPlaced();
        GeneratorUtil.setEnergyTransformer(block);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        Player player = e.getPlayer();

        // Проверяем, что это наш генератор
        if (!GeneratorUtil.isGenerator(block)) return;

        ItemStack tool = player.getInventory().getItemInMainHand();

        // Проверяем, что инструмент — кирка
        if (!ToolUtil.isPickaxe(tool)) {

                    e.setCancelled(true);
            player.sendMessage("§cГенератор можно сломать только киркой!");
            return;
        }

        // Отменяем обычный дроп
        e.setDropItems(false);
        if (block.getState() instanceof org.bukkit.block.Container cont) {
            cont.getInventory().clear();
            cont.update(true); // применить изменения к tile entity
        }
        // Удаляем блок
        block.setType(Material.AIR);

        // Дропаем предмет генератора
        ItemStack generatorItem = GeneratorUtil.createGenerator(); // но с данными блока!
        block.getWorld().dropItemNaturally(block.getLocation(), generatorItem);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        Inventory inv = e.getInventory();
        InventoryHolder holder = inv.getHolder();
        if (!(holder instanceof TileState tile)) return;
        if (tile.getType() != Material.DROPPER) return;

        String type = GeneratorUtil.getGeneratorType(tile);
        if (!"energy_transformer".equals(type)) return;

        long buf = tile.getPersistentDataContainer().getOrDefault(Keys.BUFFER, PersistentDataType.LONG, 0L);
        int freq = tile.getPersistentDataContainer().getOrDefault(Keys.FREQ, PersistentDataType.INTEGER, 5);

        if (e.getPlayer() instanceof Player p) {
            p.sendMessage(ChatColor.YELLOW + "Трансформатор: энергия=" + buf + "/" + 300
                    + " частота=" + freq);
        }
    }
    @EventHandler
    public void onClickEnergySlot(InventoryClickEvent e) {
        //System.out.println("inevent");
        Inventory top = e.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof TileState tile)) return;
        if (tile.getType() != Material.DROPPER) return;

        String type = GeneratorUtil.getGeneratorType(tile);
        if (!"energy_transformer".equals(type)) return;

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
