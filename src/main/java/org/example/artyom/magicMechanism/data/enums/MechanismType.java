package org.example.artyom.magicMechanism.data.enums;

import org.bukkit.Material;
import org.example.artyom.magicMechanism.data.enums.MechanismCategory;

// Основной enum
public enum MechanismType {
    GENERATOR(Material.DROPPER, "Генератор", MechanismCategory.GENERATOR) {
        @Override
        public String getGuiLore() {
            return "Супер мега генератор!";
        }
    },
    BARRIER(Material.BARREL, "Барьер", MechanismCategory.CONSUMER) {
        @Override
        public String getGuiLore() {
            return "Супер мега барьер!";
        }
    },
    CABLE(Material.PURPLE_STAINED_GLASS_PANE, "Кабель", MechanismCategory.CABLE) {
        @Override
        public String getGuiLore() {
            return "Супер мега кабель!";
        }
    };

    private final Material material;
    private final String displayName;
    private final MechanismCategory category;

    MechanismType(Material material, String displayName, MechanismCategory category) {
        this.material = material;
        this.displayName = displayName;
        this.category = category;
    }

    public Material getMaterial() { return material; }
    public String getDisplayName() { return displayName; }
    public MechanismCategory getCategory() { return category; }

    public boolean canConnect() { return category.canConnect(); }
    public boolean canStoreEnergy() { return category.canStoreEnergy(); }

    public boolean isConsumer() {
        return this == BARRIER;
    }

    public abstract String getGuiLore();
}
