package org.example.artyom.magicMechanism.data.interfaces;

public interface IEnergyStorage {
    /**
     * Получить текущий уровень энергии
     */
    int getEnergyLevel();

    /**
     * Установить уровень энергии
     */
    void setEnergyLevel(int level);

    /**
     * Получить максимальную вместимость
     */
    int getCapacity();

    /**
     * Добавить энергию
     * @return сколько реально добавлено
     */
    int addEnergy(int amount);

    /**
     * Забрать энергию
     * @return сколько реально забрано
     */
    int removeEnergy(int amount);

    /**
     * Проверить, может ли принять энергию
     */
    boolean canAcceptEnergy();

    /**
     * Проверить, может ли отдать энергию
     */
    boolean canProvideEnergy();
}
