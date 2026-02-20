package org.example.artyom.magicMechanism.data.enums.barrier;

import org.example.artyom.magicMechanism.data.enums.MenuAction;
import org.example.artyom.magicMechanism.mechanisms.Barrier;

import java.io.Serializable;

public class BarrierMenuActionSlot implements MenuAction {
    public static BarrierMenuActions action = BarrierMenuActions.MAIN_MENU_EDIT_PLAYER;
    int slotPlayer;
    public BarrierMenuActionSlot(int slotPlayer) {
        this.slotPlayer = slotPlayer;
    }

    public int getSlotPlayer() {return slotPlayer;}

    @Override
    public String getPdcKey() {
        return action.getPdcKey() + ":" + slotPlayer;
    }
}
