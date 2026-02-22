package org.example.artyom.magicMechanism.items;

import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.data.enums.MechanismType;

public class BarrierItem extends BaseMechanismItem {
    public BarrierItem(MagicMechanism plugin) {
        super(plugin, MechanismType.BARRIER);
    }
}
