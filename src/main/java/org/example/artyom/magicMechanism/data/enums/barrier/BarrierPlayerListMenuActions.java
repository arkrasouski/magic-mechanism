package org.example.artyom.magicMechanism.data.enums.barrier;

import org.example.artyom.magicMechanism.data.enums.MenuAction;
import org.example.artyom.magicMechanism.data.enums.ScreenCategory;

public enum BarrierPlayerListMenuActions implements ScreenCategory, MenuAction {
    PAGE,
    PREVIOUS_PAGE,
    NEXT_PAGE,
    RETURN_BACK,
    PLAYERLIST;


    @Override
    public String getPdcKey() {
        return "PLAYERLIST_" + name();
    }

    public static BarrierPlayerListMenuActions fromString(String fullName) {

        return MenuAction.fromString(fullName, BarrierPlayerListMenuActions.class);
    }


    @Override
    public String getDisplayName() {
        return "Меню добавления игрока к барьеру";
    }

    @Override
    public ScreenCategory getCategory() {
        return BarrierScreenCategory.PLAYER_LIST;
    }
}
