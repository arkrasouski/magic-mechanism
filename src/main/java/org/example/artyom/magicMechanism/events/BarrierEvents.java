package org.example.artyom.magicMechanism.events;

import org.bukkit.event.Listener;

public class BarrierEvents implements Listener {
//    @EventHandler
//    public void onPlaceBarrier(BlockPlaceEvent e) {
//
//        ItemStack item = e.getItemInHand();
//        if (!BarrierUtil.isBarrierItem(item)) return;
//
//        Block block = e.getBlockPlaced();
//        BarrierUtil.setEnergyBarrier(block);
//    }
//
//    @EventHandler(ignoreCancelled = true)
//    public void onBreak(BlockBreakEvent e) {
//        if (BarrierUtil.isDenied(e.getPlayer(), e.getBlock().getLocation())) {
//            e.setCancelled(true);
//        }
//    }
//
//    @EventHandler(ignoreCancelled = true)
//    public void onPlace(BlockPlaceEvent e) {
//        if (isDenied(e.getPlayer(), e.getBlockPlaced().getLocation())) {
//            e.setCancelled(true); // отмена запретит установку [web:44]
//        }
//    }
//
//    @EventHandler(ignoreCancelled = true)
//    public void onInteract(PlayerInteractEvent e) {
//        Block clicked = e.getClickedBlock(); // может быть null [web:41]
//        if (clicked == null) return;
//        if (isDenied(e.getPlayer(), clicked.getLocation())) {
//            e.setCancelled(true);
//        }
//    }
}
