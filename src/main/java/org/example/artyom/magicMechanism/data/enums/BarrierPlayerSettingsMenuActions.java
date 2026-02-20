package org.example.artyom.magicMechanism.data.enums;

import java.util.List;

public enum BarrierPlayerSettingsMenuActions implements Screen, MenuAction {
    PLAYER_SETTINGS_ALLOW_CHEST,
    PLAYER_SETTINGS_DENY_CHEST,
    PLAYER_SETTINGS_ALLOW_DAMAGE,
    PLAYER_SETTINGS_DENY_DAMAGE,
    PLAYER_SETTINGS_REMOVE_PLAYER,
    PLAYER_SETTINGS_RETURN;

    @Override
    public String getPdcKey() {
        return "PLAYERSETTINGS_" + name();
    }

    public static BarrierPlayerSettingsMenuActions fromString(String fullName) {
        return MenuAction.fromString(fullName, BarrierPlayerSettingsMenuActions.class);
    }


    @Override
    public List<MenuAction> getActions() {
        return List.of();
    }

    @Override
    public String getDisplayName() {
        return "Меню изменения игрока в барьере";
    }

    @Override
    public ScreenCategory getCategory() {
        return BarrierScreenCategory.PLAYER_SETTINGS;
    }
}
