package org.example.artyom.magicMechanism.data.enums.barrier;

public enum ActionPrefix {
    BARRIER,
    PLAYERLIST,
    PLAYERSETTINGS;

    public static ActionPrefix fromString(String actionStr) {
        for (ActionPrefix prefix : values()) {
            if (actionStr.startsWith(prefix.name() + "_")) {
                return prefix;
            }
        }
        return null;
    }
}
