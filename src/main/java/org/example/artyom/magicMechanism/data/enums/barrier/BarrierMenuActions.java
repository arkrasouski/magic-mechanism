package org.example.artyom.magicMechanism.data.enums.barrier;

import org.example.artyom.magicMechanism.data.enums.MenuAction;
import org.example.artyom.magicMechanism.data.enums.ScreenCategory;

public enum BarrierMenuActions implements ScreenCategory, MenuAction {

    MAIN_MENU_ADD_PLAYER,
    MAIN_MENU_EDIT_PLAYER;





    @Override
    public String getPdcKey() {
        return "BARRIER_" + name();
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

}
