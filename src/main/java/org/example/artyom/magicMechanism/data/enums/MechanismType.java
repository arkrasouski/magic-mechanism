package org.example.artyom.magicMechanism.data.enums;

public enum MechanismType {
    GENERATOR, BARRIER; // добавляйте новые

    public String getGuiTitle() {
        return switch (this) {
            case GENERATOR -> "Генератор";
            case BARRIER -> "Барьер";
        };
    }
}
