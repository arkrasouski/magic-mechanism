package org.example.artyom.magicMechanism.data.enums.barrier;

import org.example.artyom.magicMechanism.data.enums.MenuAction;
import org.example.artyom.magicMechanism.data.enums.Screen;
import org.example.artyom.magicMechanism.data.enums.ScreenCategory;

import java.util.List;

public enum BarrierPlayerListMenuActions implements Screen, MenuAction {
    PLAYER_LIST;


    @Override
    public String getPdcKey() {
        return "PLAYERLIST_" + name();
    }

    public static BarrierPlayerListMenuActions fromString(String fullName) {

        return MenuAction.fromString(fullName, BarrierPlayerListMenuActions.class);
    }

    @Override
    public List<MenuAction> getActions() {
        return List.of();
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
