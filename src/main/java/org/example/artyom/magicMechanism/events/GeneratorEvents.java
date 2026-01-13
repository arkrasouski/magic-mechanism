package org.example.artyom.magicMechanism.events;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Panda;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.example.artyom.magicMechanism.utils.GeneratorUtil;
import org.example.artyom.magicMechanism.utils.ToolUtil;

public class GeneratorEvents implements Listener {
    @EventHandler
    public void onClick(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;

        Block block = e.getClickedBlock();
        if (!GeneratorUtil.isGenerator(block)) return;

        //e.setCancelled(true);
        e.getPlayer().sendMessage("Энерго трансформатор активирован!");
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {

        ItemStack item = e.getItemInHand();
        if (!GeneratorUtil.isGeneratorItem(item)) return;

        Block block = e.getBlockPlaced();
        GeneratorUtil.markAsEnergyTransformer(block);
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

        // Удаляем блок
        block.setType(Material.AIR);

        // Дропаем предмет генератора
        ItemStack generatorItem = GeneratorUtil.createGenerator(); // но с данными блока!
        block.getWorld().dropItemNaturally(block.getLocation(), generatorItem);
    }


}
