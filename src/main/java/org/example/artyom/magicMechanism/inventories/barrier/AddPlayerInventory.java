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

    public AddPlayerInventory() {
        super(36, "add player", null);
        this.players = List.of("player1", "player2", "player3", "Florion2020");
    }

    @Override
    public Inventory openMenu(Player p, MechanismHolder holder, double percent) {
        Inventory inv = super.openMenu(p, holder, percent);


        for (int i = 0; i < 2; i++) {

            for(int j = 0; j < 8 && j < this.players.size(); j++) {
                int playerNum = j * (i+1);
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
                    inv.setItem(playerNum, ItemsUtil.create(Material.LIME_WOOL, 1,
                           this.players.get(playerNum),
                            playerFrame.getPdcKey(),
                            List.of("Игрок добавлен в приват!")));
                    continue;
                }
                }

                inv.setItem(playerNum,
                        ItemsUtil.create(Material.LIGHT_BLUE_WOOL, 1, this.players.get(playerNum),
                                playerFrame.getPdcKey(), List.of("Нажмите, чтобы добавить", "Игрока в приват")));
            }
            if(this.players.size() < 8) break;
        }
        inv.setItem(8, ItemsUtil.create(Material.OAK_DOOR, 1, "На главную", BarrierPlayerListMenuActions.RETURN_BACK.getPdcKey(), List.of("Вернуться в главное меню")));
        inv.setItem(18, ItemsUtil.create(Material.YELLOW_WOOL, 1, "Предыдущая страница", BarrierPlayerListMenuActions.PREVIOUS_PAGE.getPdcKey(), List.of("Нажмите, чтобы", "Перелистнуть назад")));
        inv.setItem(22, ItemsUtil.create(Material.PAPER, 1, "Страница № " + this.page, BarrierPlayerListMenuActions.PAGE.getPdcKey()));
        inv.setItem(26, ItemsUtil.create(Material.ORANGE_WOOL, 1, "Следующая страница", BarrierPlayerListMenuActions.NEXT_PAGE.getPdcKey(), List.of("Нажмите, чтобы", "Перелистнуть вперед")));




        return inv;
    }

}
