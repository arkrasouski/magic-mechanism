package org.example.artyom.magicMechanism.mechanisms;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.data.enums.MechanismType;
import org.example.artyom.magicMechanism.data.interfaces.IEnergyHandler;
import org.example.artyom.magicMechanism.data.records.MechanismData;
import org.example.artyom.magicMechanism.managers.BarrierManager;
import org.example.artyom.magicMechanism.utils.LogUtil;

import java.util.Map;
import java.util.UUID;

public class Barrier extends BaseMechanism {


    private static int CAPACITY = 750;
    // Конструктор для нового барьера
    public Barrier(Location location, UUID owner) {
        super(location, MechanismType.BARRIER, owner, CAPACITY);
        this.energyLevel = 0;
    }

    // Конструктор для загрузки из данных
    public Barrier(Location location, UUID owner, int energy, int capacity) {
        super(location, MechanismType.BARRIER, owner, capacity);
        this.energyLevel = energy;
    }

    // Фабричный метод для создания (при размещении)
    public static Barrier create(Location location, UUID owner, BarrierManager manager) {
        Barrier barrier = new Barrier(location, owner);
        manager.addMechanism(location, barrier);
        LogUtil.warn("✓ БАРЬЕР СОЗДАН: " + location);
        return barrier;
    }

    // Фабричный метод для загрузки (из данных)
    public static Barrier load(Location location, MechanismData data, BarrierManager manager) {
        Barrier barrier = new Barrier(location,data.owner(), data.energy(), data.maxEnergy());
        barrier.setActive(data.active());
        manager.addMechanism(location, barrier);
        LogUtil.warn("✓ БАРЬЕР ЗАГРУЖЕН: " + location + " энергия: " + data.energy());
        return barrier;
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

    @Override
    public int getCapacity() {
        return capacity;
    }
}