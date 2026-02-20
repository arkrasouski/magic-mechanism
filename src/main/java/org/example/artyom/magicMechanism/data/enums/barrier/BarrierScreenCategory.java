package org.example.artyom.magicMechanism.data.enums.barrier;

import org.example.artyom.magicMechanism.data.enums.ScreenCategory;

public enum BarrierScreenCategory implements ScreenCategory {
    MAIN_MENU("Главное меню"),
    PLAYER_LIST("Список игроков"),
    PLAYER_SETTINGS("Настройки игрока");

    private final String displayName;
    BarrierScreenCategory(String displayName) { this.displayName = displayName; }

    @Override
    public ScreenCategory getCategory() {
        System.out.println(this);
        return this;
    }

    @Override public String getDisplayName() { return displayName; }
    public boolean isBarrier() { return true; }
}
