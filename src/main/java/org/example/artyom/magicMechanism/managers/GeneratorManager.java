package org.example.artyom.magicMechanism.managers;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.data.enums.MechanismType;
import org.example.artyom.magicMechanism.data.records.MechanismData;
import org.example.artyom.magicMechanism.mechanisms.Generator;

public class GeneratorManager extends BaseManager<Generator> {

    public GeneratorManager(MagicMechanism plugin) {
        super(plugin, MechanismType.GENERATOR);
        loadAllMechanismsFromLoadedChunks();
    }

    @Override
    protected Generator createMechanismInstance(Location location, Player owner,
                                                int energy, int capacity, boolean active) {
        return new Generator(location, owner, energy, capacity, active);
    }

//    @Override
//    protected MechanismData createMechanismData(Generator generator) {
//        return new MechanismData(
//                generator.getLocation().getBlockX(),
//                generator.getLocation().getBlockY(),
//                generator.getLocation().getBlockZ(),
//                generator.getEnergyLevel(),
//                generator.getCapacity(),
//                generator.isActive(),
//                generator.getOwner() != null ? generator.getOwner().getUniqueId() : null
//        );
//    }

    @Override
    protected Generator deserializeMechanism(MechanismData data, World world) {
        Location loc = data.toLocation(world);
        Player owner = data.owner() != null ?
                plugin.getServer().getPlayer(data.owner()) : null;

        return new Generator(loc, owner, data.energy(),
                data.maxEnergy(), data.active());
    }

    @Override
    protected int getDefaultCapacity() {
        return 2000; // Генераторы могут иметь большую емкость
    }

    // Специфические методы для генераторов

    /**
     * Получает общую энергию всех генераторов
     */
    public int getTotalEnergy() {
        return getAllMechanisms().stream()
                .mapToInt(Generator::getEnergyLevel)
                .sum();
    }

    /**
     * Получает максимальную энергию всех генераторов
     */
    public int getTotalCapacity() {
        return getAllMechanisms().stream()
                .mapToInt(Generator::getCapacity)
                .sum();
    }
}