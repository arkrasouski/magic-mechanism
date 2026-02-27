package org.example.artyom.magicMechanism.data.interfaces;

import org.bukkit.Location;

import java.util.Set;

public interface IEnergyNetwork {
    /**
     * Получить все подключенные потребители
     */
    Set<Location> getConnectedConsumers();

    /**
     * Получить все подключенные генераторы
     */
    Set<Location> getConnectedGenerators();

    /**
     * Передать энергию по сети
     * @return количество переданной энергии
     */
    int transferEnergy(int amount, Location from);

    /**
     * Проверить, является ли блок частью сети
     */
    boolean isInNetwork(Location loc);

    /**
     * Получить все соединения
     */
    Set<Location> getAllConnections();
}
