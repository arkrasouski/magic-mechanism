package org.example.artyom.magicMechanism.data.interfaces;

public interface IEnergyConsumer {
    /**
     * Потребить энергию (использовать для функционала)
     */
    void consumeEnergy(int amount);

    /**
     * Получить скорость потребления
     */
    int getConsumptionRate();

    /**
     * Проверить, нужна ли энергия сейчас
     */
    boolean needsEnergy();
}
