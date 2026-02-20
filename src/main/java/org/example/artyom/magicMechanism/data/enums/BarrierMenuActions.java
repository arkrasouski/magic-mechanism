package org.example.artyom.magicMechanism.data.enums;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.data.Keys;

import java.util.Arrays;
import java.util.List;

public enum BarrierMenuActions implements Screen, MenuAction {
//
//    MAIN_MENU_ADD_PLAYER("main_menu_add_player"),
//    MAIN_MENU_EDIT_PLAYER("main_menu_edit_player"),
//    PLAYER_LIST("player_list"),
//    PLAYER_SETTINGS_ALLOW_CHEST("player_settings_allow_chest"),
//    PLAYER_SETTINGS_DENY_CHEST("player_settings_deny_chest"),
//    PLAYER_SETTINGS_ALLOW_DAMAGE("player_settings_allow_damage"),
//    PLAYER_SETTINGS_DENY_DAMAGE("player_settings_deny_damage"),
//    PLAYER_SETTINGS_REMOVE_PLAYER("player_settings_remove_player"),
//    PLAYER_SETTINGS_RETURN("player_settings_return");
//    public final String menuName;
//    public static final String prefix = "Barrier_";
//    BarrierMenuActions(String menuName){
//        this.menuName = prefix + menuName;
//    }
//
//    public String getMenuName() { return menuName; }
//
//    public static BarrierMenuActions fromMenuName(String name) {
//        return Arrays.stream(values())
//                .filter(v -> v.menuName.equals(name))
//                .findFirst()
//                .orElse(null);
//    }
//
//    public static BarrierMenuActions getActionFromPDC(ItemStack item) {
//        if (item == null || !item.hasItemMeta()) {
//            return null;
//        }
//
//        ItemMeta meta = item.getItemMeta();
//        PersistentDataContainer pdc = meta.getPersistentDataContainer();
//
//        String actionStr = pdc.get(Keys.INVENTORY_ITEM, PersistentDataType.STRING);
//
//        if (actionStr == null) {
//            return null;
//        }
//
//        try {
//            return BarrierMenuActions.valueOf(actionStr.split("BARRIER_")[1]);
//        } catch (IllegalArgumentException e) {
//            MagicMechanism.getInstance().getLogger().warning("Invalid enum in PDC: " + actionStr);
//            return null;
//        }
//    }
//
//    @Override
//    public String getDisplayName() {
//        return "Барьер";
//    }
//
//    @Override
//    public boolean isBarrier() {
//        return true;
//    }
    MAIN_MENU_ADD_PLAYER,
    MAIN_MENU_EDIT_PLAYER;

    @Override
    public String getPdcKey() {
        return "BARRIER_" + name();
    }

    public String getPdcKey(int i) {
        return "BARRIER_" + name() + "_" + i;
    }

    public static BarrierMenuActions fromString(String fullName) {
        return MenuAction.fromString(fullName, BarrierMenuActions.class);
    }

    @Override
    public ScreenCategory getCategory() {
        return BarrierScreenCategory.MAIN_MENU;
    }

    @Override
    public String getDisplayName() {
        return "Барьер-меню";
    }

    @Override
    public List<MenuAction> getActions() {
        return List.of();
    }
}
