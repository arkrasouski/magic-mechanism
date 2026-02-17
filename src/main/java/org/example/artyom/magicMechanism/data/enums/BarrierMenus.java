package org.example.artyom.magicMechanism.data.enums;

public enum BarrierMenus {

    MAIN_MENU( "main_menu"),
    PLAYER_LIST("player_list"),
    PLAYER_SETTINGS("player_settings");

    public final String menuName;
    public static final String prefix = "Barrier_";
    BarrierMenus(String menuName){
        this.menuName = prefix + menuName;
    }
}
