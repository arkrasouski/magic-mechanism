package org.example.artyom.magicMechanism.mechanisms;
import org.bukkit.Location;
import org.example.artyom.magicMechanism.data.enums.MechanismType;
import org.example.artyom.magicMechanism.data.records.MechanismData;
import org.example.artyom.magicMechanism.managers.GeneratorManager;
import org.example.artyom.magicMechanism.utils.LogUtil;

import java.util.UUID;


public class Generator extends BaseMechanism {

    public static final int CAPACITY = 1000;
    public static final int frequency = 10;
    public static final int frequencySpeed = 20;
    public Generator(Location location, UUID owner) {
        super(location, MechanismType.GENERATOR, owner, CAPACITY);
        this.energyLevel = 0;
    }

    // Конструктор для загрузки из данных
    public Generator(Location location, UUID owner, int energy, int capacity) {
        super(location, MechanismType.GENERATOR, owner, capacity);
        this.energyLevel = energy;
    }

    // Фабричный метод для создания (при размещении)
    public static Generator create(Location location, UUID owner, GeneratorManager manager) {
        Generator generator = new Generator(location, owner);
        manager.addMechanism(location, generator);
        LogUtil.warn("✓ ГЕНЕРАТОР СОЗДАН: " + location);
        return generator;
    }

    // Фабричный метод для загрузки (из данных)
    public static Generator load(Location location, MechanismData data, GeneratorManager manager) {
        Generator generator = new Generator(location,data.owner(), data.energy(), data.maxEnergy());
        generator.setActive(data.active());
        manager.addMechanism(location, generator);
        LogUtil.warn("✓ ГЕНЕРАТОР ЗАГРУЖЕН: " + location + " энергия: " + data.energy());
        return generator;
    }

    @Override
    public MechanismData toData() {
        return new MechanismData(
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                energyLevel,
                capacity,
                active,
                owner
        );
    }

    @Override
    public int getEnergyLevel() {
        return energyLevel;
    }

    @Override
    public void setEnergyLevel(int energy) {
        this.energyLevel = Math.min(energy, capacity);
    }

    public boolean transferEnergy(int amount) {
        if (amount <= 0) return false;
        if (energyLevel < amount) return false;

        energyLevel -= amount;
        LogUtil.warn("Генератор передал " + amount + " энергии. Осталось: " + energyLevel);
        return true;
    }

}
