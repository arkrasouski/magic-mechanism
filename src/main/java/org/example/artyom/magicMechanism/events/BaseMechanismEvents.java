package org.example.artyom.magicMechanism.events;
import org.bukkit.Material;
import org.bukkit.block.Block;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import org.bukkit.inventory.ItemStack;

import org.example.artyom.magicMechanism.mechanisms.BaseMechanism;
import org.example.artyom.magicMechanism.utils.ToolUtil;

public abstract class BaseMechanismEvents implements Listener {
    BaseMechanism mechanism;

    public BaseMechanismEvents(BaseMechanism mechanism) {
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
}
