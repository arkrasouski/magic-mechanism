package org.example.artyom.magicMechanism.events;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import org.example.artyom.magicMechanism.mechanisms.Barrier;


public class BarrierEvents extends BaseMechanismEvents {

    public BarrierEvents() {
        super(new Barrier());
    }
    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block clicked = e.getClickedBlock(); // может быть null [web:41]
        if (clicked == null) return;

        if (Barrier.isBarrier(clicked)) {
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
