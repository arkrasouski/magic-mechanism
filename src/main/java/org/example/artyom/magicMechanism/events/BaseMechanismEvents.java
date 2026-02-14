package org.example.artyom.magicMechanism.events;
import org.bukkit.Material;
import org.bukkit.block.Block;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import org.bukkit.inventory.ItemStack;

import org.example.artyom.magicMechanism.utils.BaseMechanismUtil;
import org.example.artyom.magicMechanism.utils.ToolUtil;

public abstract class BaseMechanismEvents implements Listener {
    BaseMechanismUtil mechanism;

    public BaseMechanismEvents(BaseMechanismUtil mechanism) {
        this.mechanism = mechanism;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {

        ItemStack item = e.getItemInHand();
        if (!mechanism.isMechanismItem(item)) return;

        Block block = e.getBlockPlaced();
        mechanism.setMechanismBlock(block);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        Player player = e.getPlayer();

        // Проверяем, что это наш генератор
        if (!mechanism.isMechanismBlock(block)) return;

        ItemStack tool = player.getInventory().getItemInMainHand();

        // Проверяем, что инструмент — кирка
        if (!ToolUtil.canBreakWithTool(player, tool)) {

            e.setCancelled(true);
            player.sendMessage("§c" + mechanism.getName() + " можно сломать только киркой!");
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
        ItemStack mechanismItem = mechanism.createMechanismItem(); // но с данными блока!
        block.getWorld().dropItemNaturally(block.getLocation(), mechanismItem);
    }

//    @EventHandler
//    public void onInventoryOpen(InventoryOpenEvent e) {
//        Inventory inv = e.getInventory();
//        InventoryHolder holder = inv.getHolder();
//        if (!(holder instanceof TileState tile)) return;
//        if (tile.getType() != Material.DROPPER) return;
//
//        String type = GeneratorUtil.getGeneratorType(tile);
//        if (!"energy_transformer".equals(type)) return;
//
//        long buf = tile.getPersistentDataContainer().getOrDefault(Keys.BUFFER, PersistentDataType.LONG, 0L);
//        int freq = tile.getPersistentDataContainer().getOrDefault(Keys.FREQ, PersistentDataType.INTEGER, 5);
//
//        if (e.getPlayer() instanceof Player p) {
//            p.sendMessage(ChatColor.YELLOW + "Трансформатор: энергия=" + buf + "/" + 300
//                    + " частота=" + freq);
//        }
//    }
//    @EventHandler
//    public void onClickEnergySlot(InventoryClickEvent e) {
//        //System.out.println("inevent");
//        Inventory top = e.getView().getTopInventory();
//        InventoryHolder holder = top.getHolder();
//        if (!(holder instanceof TileState tile)) return;
//        if (tile.getType() != Material.DROPPER) return;
//
//        String type = GeneratorUtil.getGeneratorType(tile);
//        if (!"energy_transformer".equals(type)) return;
//
//        // Клик по верхнему инвентарю (сам дроппер)
//        if (e.getClickedInventory() == null) return;
//        if (!e.getClickedInventory().equals(top)) return;
//
//        if (e.getSlot() != 0) return; // первый слот дроппера
//        //System.out.println("Dropper");
//        // ВАЖНО: во время InventoryClickEvent инвентарь ещё "старый".
//        // Поэтому проверяем результат на следующий тик.
//        Bukkit.getScheduler().runTask(MagicMechanism.getInstance(), () -> {
//            ItemStack inSlot = top.getItem(0);
//            if (EnergyCellUtil.isEnergyCell(inSlot)) {
//                //System.out.println("isEnergyCell");
//                Player p = (Player) e.getWhoClicked();
//                p.sendMessage("Аккумулятор вставлен в слот 1!");
//                // тут можешь запускать логику зарядки/линка
//                GeneratorService.onCellInserted(tile.getLocation());
//            }
//            else {
//                GeneratorService.onCellRemoved(tile.getLocation());
//            }
//        });
//    }
}
