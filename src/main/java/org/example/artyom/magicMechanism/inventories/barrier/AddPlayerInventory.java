package org.example.artyom.magicMechanism.inventories.barrier;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.data.enums.barrier.BarrierPlayerListMenuActionSlot;
import org.example.artyom.magicMechanism.data.enums.barrier.BarrierPlayerListMenuActions;
import org.example.artyom.magicMechanism.inventories.BaseFillCustomInventory;
import org.example.artyom.magicMechanism.inventories.MechanismHolder;
import org.example.artyom.magicMechanism.utils.ItemsUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AddPlayerInventory extends BaseFillCustomInventory {

    List<String> players = new ArrayList<>();
    int page = 1;

    public AddPlayerInventory(int page, ArrayList<String> players) {
        super(36, "add player", null);
        this.players = players;
        this.page = page;
    }

    @Override
    public Inventory openMenu(Player p, MechanismHolder holder, double percent) {
        Inventory inv = super.openMenu(p, holder, percent);


        for (int i = 0; i < 2; i++) {

            for(int j = 0; j < this.players.size() && j < 7 ; j++) {
                int playerNum = j * (i+1);
                int slot = j + i * 9;
                BarrierPlayerListMenuActionSlot playerFrame = new BarrierPlayerListMenuActionSlot(playerNum);
                if(holder.getLocation().getBlock().getState() instanceof TileState tile) {
                    boolean hasData = false ;
                for(int k = 0; k < 12; k++) {
                    String namespace = "magic-mechanism";
                    String key = "barrier_added_player_names" + "_" + k;
                    NamespacedKey checkKey = new NamespacedKey(namespace, key);
                    if(tile.getPersistentDataContainer().has(checkKey) &&
                            Objects.equals(tile.getPersistentDataContainer().get(checkKey, PersistentDataType.STRING), players.get(playerNum))) {
                        hasData = true;
                        break;
                    }
                }

                if(hasData) {
                    inv.setItem(slot, ItemsUtil.create(Material.LIME_WOOL, 1,
                           this.players.get(playerNum),
                            playerFrame.getPdcKey(),
                            List.of("Игрок добавлен в приват!")));
                    continue;
                }
                }

                inv.setItem(slot,
                        ItemsUtil.create(Material.LIGHT_BLUE_WOOL, 1, this.players.get(playerNum),
                                playerFrame.getPdcKey(), List.of("Нажмите, чтобы добавить", "Игрока в приват")));
            }
            if(this.players.size() < 7) break;
        }
        inv.setItem(8, ItemsUtil.create(Material.OAK_DOOR, 1, "На главную", BarrierPlayerListMenuActions.RETURN_BACK.getPdcKey(), List.of("Вернуться в главное меню")));
        if(this.page > 1) {
            inv.setItem(18, ItemsUtil.create(Material.YELLOW_WOOL, 1, String.format("Страница %d", this.page-1), BarrierPlayerListMenuActions.PREVIOUS_PAGE.getPdcKey(), List.of("Нажмите, чтобы", "Перелистнуть назад")));
        }
        inv.setItem(22, ItemsUtil.create(Material.PAPER, 1, "Страница № " + this.page, BarrierPlayerListMenuActions.PAGE.getPdcKey()));

        if(this.players.size() == 16) {
            inv.setItem(26, ItemsUtil.create(Material.ORANGE_WOOL, 1, String.format("Страница %d", this.page+1), BarrierPlayerListMenuActions.NEXT_PAGE.getPdcKey(), List.of("Нажмите, чтобы", "Перелистнуть вперед")));
        }



        return inv;
    }

}
