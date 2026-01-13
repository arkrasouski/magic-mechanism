package org.example.artyom.magicMechanism.events;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.example.artyom.magicMechanism.utils.GeneratorUtil;

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


}
