package org.example.artyom.magicMechanism.data.enums.barrier;

import org.example.artyom.magicMechanism.data.enums.MenuAction;

public class BarrierPlayerListMenuActionSlot implements MenuAction {
    public static BarrierPlayerListMenuActions action = BarrierPlayerListMenuActions.PLAYERLIST;
    private int slotPlayer;

    public BarrierPlayerListMenuActionSlot(int slotPlayer) {
        this.slotPlayer = slotPlayer;
    }
    public int getSlotPlayer() {return slotPlayer;}
    @Override
    public String getPdcKey() {
        return action.getPdcKey() + ":" + slotPlayer;
    }
}
