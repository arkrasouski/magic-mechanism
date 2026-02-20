package org.example.artyom.magicMechanism.inventories.barrier;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.example.artyom.magicMechanism.data.enums.barrier.BarrierMenuActionSlot;
import org.example.artyom.magicMechanism.data.enums.barrier.BarrierMenuActions;

import org.example.artyom.magicMechanism.inventories.BaseFillCustomInventory;
import org.example.artyom.magicMechanism.inventories.MechanismHolder;
import org.example.artyom.magicMechanism.utils.ItemsUtil;

import java.util.List;

public class BarrierInventory extends BaseFillCustomInventory {

    public BarrierInventory() {
        super(36, "&f:offset_-64::barrier_menu::offset_64:", null);
    }

    @Override
    public Inventory openMenu(Player p, MechanismHolder holder, double percent) {
        Inventory inv = super.openMenu(p, holder, percent);
        //inv.setItem();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                if(j < 4) {
                    int num = (j+1) + 4  * i;
                    BarrierMenuActionSlot action = new BarrierMenuActionSlot(num);
                    inv.setItem(j + (9 * i), ItemsUtil.create(Material.BLUE_WOOL, 1,
                            "Игрок №" + num,
                            action.getPdcKey(),
                            List.of("Добавить")));
                }


            }
        }
        inv.setItem(14, ItemsUtil.create(Material.GREEN_WOOL, 1,
                "Добавить игрока", BarrierMenuActions.MAIN_MENU_ADD_PLAYER.getPdcKey(),
                List.of("Нажмите, чтобы добавить игрока в приват")));

        return inv;
    }
}
