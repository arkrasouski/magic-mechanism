package org.example.artyom.magicMechanism.network;

import org.bukkit.Location;
import org.example.artyom.magicMechanism.data.interfaces.IEnergyHandler;
import org.example.artyom.magicMechanism.managers.EnergyManager;

import java.util.HashSet;
import java.util.Set;

public class EnergyNetwork {
    private final Set<Location> networkBlocks = new HashSet<>();
    private final Set<Location> generators = new HashSet<>();
    private final Set<Location> machines = new HashSet<>();
    private final Set<Location> cables = new HashSet<>();
    private final Set<Location> batteries = new HashSet<>();

    /**
     * Добавить блок в сеть.
     */
    public void addBlock(Location loc, IEnergyHandler handler) {
        if (networkBlocks.add(loc)) {
            categorizeBlock(loc, handler);
        }
    }

    /**
     * Удалить блок из сети.
     */
    public void removeBlock(Location loc) {
        networkBlocks.remove(loc);
        generators.remove(loc);
        machines.remove(loc);
        cables.remove(loc);
        batteries.remove(loc);
    }

    /**
     * Объединить две сети в одну.
     */
    public void merge(EnergyNetwork other) {
        for (Location loc : other.networkBlocks) {
            this.addBlock(loc, EnergyManager.getHandler(loc)); // EnergyManager будет хранить обработчики
        }
    }

    private void categorizeBlock(Location loc, IEnergyHandler handler) {
        if (handler.canExtract() && !handler.canReceive()) {
            generators.add(loc); // Чистый генератор
        } else if (handler.canReceive() && !handler.canExtract()) {
            machines.add(loc); // Чистый потребитель
        } else if (handler.canExtract() && handler.canReceive()) {
            batteries.add(loc); // Может и принимать, и отдавать (батарея)
        } else {
            cables.add(loc); // Только провод (не производит и не потребляет)
        }
    }

    /**
     * Главный метод для передачи энергии.
     * Вызывается тикером.
     */
    public void tick() {
        // 1. Сначала собираем всю доступную энергию с генераторов
        double availableEnergy = 0;
        for (Location genLoc : generators) {
            IEnergyHandler gen = EnergyManager.getHandler(genLoc);
            if (gen != null && gen.canExtract()) {
                // Забираем ВСЮ энергию из генератора. В симуляции мы не забираем, а только считываем.
                availableEnergy += gen.extractEnergy(gen.getEnergyStored());
            }
        }

        // 2. Собираем энергию с батарей (они тоже могут отдавать)
        for (Location batLoc : batteries) {
            IEnergyHandler bat = EnergyManager.getHandler(batLoc);
            if (bat != null && bat.canExtract()) {
                availableEnergy += bat.extractEnergy(bat.getEnergyStored());
            }
        }

        // 3. Если энергии нет, выходим
        if (availableEnergy <= 0) return;

        // 4. Считаем общий запрос (сколько энергии нужно всем машинам и батареям)
        double totalDemand = 0;
        for (Location machineLoc : machines) {
            IEnergyHandler machine = EnergyManager.getHandler(machineLoc);
            if (machine != null && machine.canReceive()) {
                double needed = machine.getMaxEnergyStored() - machine.getEnergyStored();
                totalDemand += needed;
            }
        }
        for (Location batLoc : batteries) {
            IEnergyHandler bat = EnergyManager.getHandler(batLoc);
            if (bat != null && bat.canReceive()) {
                double needed = bat.getMaxEnergyStored() - bat.getEnergyStored();
                totalDemand += needed;
            }
        }

        // 5. Распределяем энергию пропорционально запросу
        if (totalDemand > 0) {
            double energyToDistribute = Math.min(availableEnergy, totalDemand);

            // Сначала отдаем машинам
            for (Location machineLoc : machines) {
                IEnergyHandler machine = EnergyManager.getHandler(machineLoc);
                if (machine != null && machine.canReceive()) {
                    double needed = machine.getMaxEnergyStored() - machine.getEnergyStored();
                    if (needed > 0) {
                        double share = (needed / totalDemand) * energyToDistribute;
                        double accepted = machine.receiveEnergy(share);
                        availableEnergy -= accepted;
                    }
                }
            }

            // Остаток (если есть) отдаем батареям
            if (availableEnergy > 0) {
                for (Location batLoc : batteries) {
                    IEnergyHandler bat = EnergyManager.getHandler(batLoc);
                    if (bat != null && bat.canReceive()) {
                        double needed = bat.getMaxEnergyStored() - bat.getEnergyStored();
                        if (needed > 0) {
                            double share = Math.min(availableEnergy, needed);
                            double accepted = bat.receiveEnergy(share);
                            availableEnergy -= accepted;
                        }
                    }
                }
            }
        }

        // 6. Теперь, когда мы знаем, сколько реально ушло машинам/батареям,
        // нужно физически списать эту энергию с генераторов и батарей-доноров.
        // Это сложная часть: нужно понять, откуда именно брать энергию.
        // В этом упрощенном примере мы НЕ реализуем алгоритм выбора источника.
        // В реальном плагине вам нужно будет, например, равномерно распределять
        // нагрузку или иметь приоритеты (генераторы -> батареи).
        //deductEnergyFromSources(energyToDistribute); // Воображаемый метод
    }
}
