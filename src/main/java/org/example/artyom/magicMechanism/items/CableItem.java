package org.example.artyom.magicMechanism.items;

import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.data.enums.MechanismType;

public class CableItem extends BaseMechanismItem{
    public CableItem(MagicMechanism plugin) {
        super(plugin, MechanismType.CABLE)
        ;
    }
}
