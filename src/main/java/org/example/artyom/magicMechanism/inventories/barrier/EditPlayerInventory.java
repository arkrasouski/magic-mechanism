package org.example.artyom.magicMechanism.inventories.barrier;

import org.bukkit.Material;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.data.Keys;
import org.example.artyom.magicMechanism.inventories.BaseFillCustomInventory;
import org.example.artyom.magicMechanism.inventories.MechanismHolder;
import org.example.artyom.magicMechanism.inventories.barrier.holders.EditPlayerHolder;
import org.example.artyom.magicMechanism.utils.ItemsUtil;
import org.example.artyom.magicMechanism.utils.LogUtil;

import java.util.List;

import static org.example.artyom.magicMechanism.data.enums.barrier.BarrierPlayerSettingsMenuActions.*;

public class EditPlayerInventory extends BaseFillCustomInventory {


    public EditPlayerInventory() {
        super(27, "glif", null);
    }

    @Override
    public Inventory openMenu(Player p, MechanismHolder holder, double percent) {
        Inventory inv = super.openMenu(p, holder, percent);

        if(holder.getLocation().getBlock().getState() instanceof TileState tile && holder instanceof EditPlayerHolder eph) {
            PersistentDataContainer pdc = tile.getPersistentDataContainer();
            int index = eph.getSlot() - 1;
            if(pdc.has(Keys.BARRIER_ALLOW_CHEST[index]) && pdc.getOrDefault(Keys.BARRIER_ALLOW_CHEST[index], PersistentDataType.BOOLEAN, false)) {
                inv.setItem(0, ItemsUtil.create(Material.PINK_WOOL, 1, "Запрет открывать сундуки",
                        PLAYER_SETTINGS_DENY_CHEST.getPdcKey(),
                        List.of("Нажмите, чтобы запретить", "использовать сундуки, печки и т.д."))
                );


            }
            else {
                inv.setItem(0, ItemsUtil.create(Material.LIME_WOOL, 1,
                        "Доступ к сундукам", PLAYER_SETTINGS_ALLOW_CHEST.getPdcKey(),
                        List.of("Нажмите, чтобы разрешить", "использовать сундуки, печки и т.д.")));
            }
        }

        inv.setItem(1, ItemsUtil.create(Material.GREEN_WOOL, 1,
                "Доступ к нанесению урона", PLAYER_SETTINGS_ALLOW_DAMAGE.getPdcKey(),
                List.of("Нажмите, чтобы разрешить", "наносить урон")));
        inv.setItem(8, ItemsUtil.create(Material.REDSTONE_BLOCK, 1,
                "Удалить игрока из привата", PLAYER_SETTINGS_REMOVE_PLAYER.getPdcKey(),
                List.of("Нажмите, чтобы удалить", "игрока из привата")));
//        inv.setItem(9, ItemsUtil.create(Material.PINK_WOOL, 1,
//                "Запрет открывать сундуки", PLAYER_SETTINGS_DENY_CHEST.getPdcKey(),
//                List.of("Нажмите, чтобы запретить", "использовать сундуки, печки и т.д.")));
//        inv.setItem(10, ItemsUtil.create(Material.RED_WOOL, 1,
//                "Запрет наносить урон", PLAYER_SETTINGS_DENY_DAMAGE.getPdcKey(),
//                List.of("Нажмите, чтобы запретить", "наносить урон")));
        inv.setItem(17, ItemsUtil.create(Material.OAK_DOOR, 1,
                "Назад", PLAYER_SETTINGS_RETURN.getPdcKey(),
                List.of("Нажмите, чтобы вернуться", "в главное меню барьера")));
        return inv;
    }
}
