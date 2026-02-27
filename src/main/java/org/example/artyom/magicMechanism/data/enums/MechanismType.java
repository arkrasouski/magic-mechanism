package org.example.artyom.magicMechanism.data.enums;

import org.bukkit.Material;

public enum MechanismType {
    GENERATOR(Material.DROPPER),
    BARRIER(Material.BARREL),
    CABLE(Material.PURPLE_STAINED_GLASS_PANE); // добавляйте новые

    private final Material material;

    MechanismType(Material material) {
        this.material = material;
    }

    public Material getMaterial() {return material;}

    public String getGuiTitle() {
        return switch (this) {
            case GENERATOR -> "Генератор";
            case BARRIER -> "Барьер";
            case CABLE -> "Кабель";
        };
    }

    public String getGuiLore() {
        return  switch (this) {
            case GENERATOR -> "Генерирует энергию из кристаллов!";
            case BARRIER -> "Генерирует защитный купол на территории";
            case CABLE -> "Передает энергию между механизмами";
        };
    }
}
