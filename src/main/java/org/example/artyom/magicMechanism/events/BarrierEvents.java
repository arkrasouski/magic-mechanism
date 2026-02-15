package org.example.artyom.magicMechanism.events;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.example.artyom.magicMechanism.data.GeneratorGuiManager;
import org.example.artyom.magicMechanism.service.GeneratorService;
import org.example.artyom.magicMechanism.utils.BarrierUtil;
import org.example.artyom.magicMechanism.utils.GeneratorUtil;

public class BarrierEvents extends BaseMechanismEvents {

    public BarrierEvents() {
        super(new BarrierUtil());
    }
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
    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block clicked = e.getClickedBlock(); // может быть null [web:41]
        if (clicked == null) return;

        if (BarrierUtil.isBarrier(clicked)) {
            if(e.getPlayer() instanceof Player p) {
                BlockState state = clicked.getState();
                if (!(state instanceof TileState tile)) return;
                    // Это НЕ tile-entity блок (например BARRIER), PDC тут не будет

                p.sendMessage(ChatColor.YELLOW + this.mechanism.getName() + ": энергия=" + this.mechanism.getCurrentEnergy(tile) + "/" + this.mechanism.getCapacity()
                        + " частота=" + this.mechanism.getFrequency());
               // p.openInventory(gui);
            }
        }
    }
}
