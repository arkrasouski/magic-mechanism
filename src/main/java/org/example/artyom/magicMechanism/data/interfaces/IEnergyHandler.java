package org.example.artyom.magicMechanism.data.interfaces;

import org.bukkit.Location;
import org.bukkit.block.Block;

public interface IEnergyHandler {
    /**
     * Может ли этот блок отдавать энергию?
     */
    boolean canExtract();

    /**
     * Может ли этот блок принимать энергию?
     */
    boolean canReceive();

    /**
     * Забрать энергию из блока (например, из генератора).
     * @param maxAmount Максимальное количество для забора.
     * @return Реальное количество забранной энергии.
     */
    double extractEnergy(double maxAmount);

    /**
     * Отдать энергию блоку (например, в механизм).
     * @param maxAmount Максимальное количество для передачи.
     * @return Реальное количество принятой энергии.
     */
    double receiveEnergy(double maxAmount);

    /**
     * Получить текущее количество энергии в блоке.
     */
    double getEnergyStored();

    /**
     * Получить максимальную вместимость блока.
     */
    double getMaxEnergyStored();

    /**
     * Получить блок в мире, к которому относится этот обработчик.
     */
    Location getLoc();
}
