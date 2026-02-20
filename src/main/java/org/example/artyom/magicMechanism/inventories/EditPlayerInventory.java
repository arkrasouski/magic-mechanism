package org.example.artyom.magicMechanism.inventories;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.example.artyom.magicMechanism.data.enums.BarrierPlayerSettingsMenuActions;

import org.example.artyom.magicMechanism.utils.ItemsUtil;

import java.util.List;

import static org.example.artyom.magicMechanism.data.enums.BarrierPlayerSettingsMenuActions.*;

public class EditPlayerInventory extends BaseFillCustomInventory{


    public EditPlayerInventory() {
        super(27, "glif", null);
    }

    @Override
    public Inventory openMenu(Player p, MechanismHolder holder, double percent) {
        Inventory inv = super.openMenu(p, holder, percent);

        inv.setItem(0, ItemsUtil.create(Material.LIME_WOOL, 1,
                "Доступ к сундукам", PLAYER_SETTINGS_ALLOW_CHEST.getPdcKey(),
                List.of("Нажмите, чтобы разрешить", "использовать сундуки, печки и т.д.")));
        inv.setItem(1, ItemsUtil.create(Material.GREEN_WOOL, 1,
                "Доступ к нанесению урона", PLAYER_SETTINGS_ALLOW_DAMAGE.getPdcKey(),
                List.of("Нажмите, чтобы разрешить", "наносить урон")));
        inv.setItem(8, ItemsUtil.create(Material.REDSTONE_BLOCK, 1,
                "Удалить игрока из привата", PLAYER_SETTINGS_REMOVE_PLAYER.getPdcKey(),
                List.of("Нажмите, чтобы удалить", "игрока из привата")));
        inv.setItem(9, ItemsUtil.create(Material.PINK_WOOL, 1,
                "Запрет открывать сундуки", PLAYER_SETTINGS_DENY_CHEST.getPdcKey(),
                List.of("Нажмите, чтобы запретить", "использовать сундуки, печки и т.д.")));
        inv.setItem(10, ItemsUtil.create(Material.RED_WOOL, 1,
                "Запрет наносить урон", PLAYER_SETTINGS_DENY_DAMAGE.getPdcKey(),
                List.of("Нажмите, чтобы запретить", "наносить урон")));
        inv.setItem(17, ItemsUtil.create(Material.OAK_DOOR, 1,
                "Назад", PLAYER_SETTINGS_RETURN.getPdcKey(),
                List.of("Нажмите, чтобы вернуться", "в главное меню барьера")));
        return inv;
    }
}
