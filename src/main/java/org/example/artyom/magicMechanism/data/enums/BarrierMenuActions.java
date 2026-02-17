package org.example.artyom.magicMechanism.data.enums;

public enum BarrierMenuActions {

    MAIN_MENU_ADD_PLAYER("main_menu_add_player"),
    MAIN_MENU_EDIT_PLAYER("main_menu_edit_player"),
    PLAYER_LIST("player_list"),
    PLAYER_SETTINGS_ALLOW_CHEST("player_settings_allow_chest"),
    PLAYER_SETTINGS_DENY_CHEST("player_settings_deny_chest"),
    PLAYER_SETTINGS_ALLOW_DAMAGE("player_settings_allow_damage"),
    PLAYER_SETTINGS_DENY_DAMAGE("player_settings_deny_damage"),
    PLAYER_SETTINGS_REMOVE_PLAYER("player_settings_remove_player"),
    PLAYER_SETTINGS_RETURN("player_settings_return");
    public final String menuName;
    public static final String prefix = "Barrier_";
    BarrierMenuActions(String menuName){
        this.menuName = prefix + menuName;
    }


}
