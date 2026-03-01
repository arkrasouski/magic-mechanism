package org.example.artyom.magicMechanism.data.interfaces;

import org.bukkit.Location;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface INetworkManager {
    /**
     * Построить сеть от начальной точки
     */
    IEnergyNetwork buildNetwork(Location start);

    /**
     * Получить сеть для компонента
     */
    IEnergyNetwork getNetwork(Location component);

    /**
     * Обновить сеть после изменений
     */
    void updateNetwork(Location changed);

    /**
     * Объединить две сети
     */
    IEnergyNetwork mergeNetworks(UUID network1, UUID network2);

    /**
     * Разделить сеть (при удалении компонента)
     */
    Set<IEnergyNetwork> splitNetwork(UUID networkId, Location removed);

    /**
     * Проверить, есть ли путь между двумя точками
     */
    boolean hasPath(Location from, Location to);

    /**
     * Получить все сети в мире
     */
    Collection<? extends IEnergyNetwork> getAllNetworks();

    /**
     * Сохранить состояние сетей
     */
    void saveNetworks();

    /**
     * Загрузить состояние сетей
     */
    void loadNetworks();
}
