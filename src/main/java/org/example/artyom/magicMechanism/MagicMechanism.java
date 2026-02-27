package org.example.artyom.magicMechanism;

import org.bukkit.Bukkit;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.example.artyom.magicMechanism.commands.GeneratorCommands;
import org.example.artyom.magicMechanism.data.GeneratorGuiManager;
import org.example.artyom.magicMechanism.data.Keys;
import org.example.artyom.magicMechanism.database.DatabaseManager;
//import org.example.artyom.magicMechanism.events.BarrierEvents;
//import org.example.artyom.magicMechanism.events.GeneratorEvents;
import org.example.artyom.magicMechanism.events.BarrierEvents;
import org.example.artyom.magicMechanism.events.CableEvents;
import org.example.artyom.magicMechanism.events.GeneratorEvents;
import org.example.artyom.magicMechanism.inventories.barrier.BarrierInventory;
import org.example.artyom.magicMechanism.inventories.generator.GenInventory;
//import org.example.artyom.magicMechanism.linkservice.GeneratorBarrierService;
//import org.example.artyom.magicMechanism.linkservice.GeneratorCellService;
import org.example.artyom.magicMechanism.linkservice.GeneratorBarrierService;
import org.example.artyom.magicMechanism.linkservice.GeneratorCellService;
import org.example.artyom.magicMechanism.managers.BarrierManager;
import org.example.artyom.magicMechanism.managers.CableManager;
import org.example.artyom.magicMechanism.managers.GeneratorManager;
import org.example.artyom.magicMechanism.mechanisms.BaseMechanism;
import org.example.artyom.magicMechanism.mechanisms.Generator;
import org.example.artyom.magicMechanism.utils.LogUtil;

import java.sql.Connection;
import java.sql.SQLException;

public final class MagicMechanism extends JavaPlugin {

    private BukkitTask tickAllTask;
    private BukkitTask tickGuiTask;
    private static MagicMechanism instance;
    private DatabaseManager databaseManager;
    private GeneratorBarrierService energyTicker;
    private GeneratorManager genManager;
    private BarrierManager barrierManager;
    private CableManager cableManager;
    @Override
    public void onEnable() {



        // Сохраняем конфиг по умолчанию из resources
        saveDefaultConfig();
        // Перезагружаем конфиг (на всякий случай)
        reloadConfig();





        // Plugin startup logic
        instance = this;
        LogUtil.init(this);
        Keys.init(this);

        //БД
        String password = getConfig().getString("database.password",
                System.getenv("MYSQL_PASSWORD") != null ? System.getenv("MYSQL_PASSWORD") : "default");
        getLogger().info("Attempting to connect with password: " + password);
        databaseManager = new DatabaseManager("localhost", 3306, "minecraft", "spigot", "spig0tDBpass!");//password);//"spig0tDBpass!");

        //Managers
        genManager = new GeneratorManager(this);
        barrierManager = new BarrierManager(this);
        GeneratorGuiManager guiManager = new GeneratorGuiManager();
        cableManager = new CableManager(this);


        //transfer
        GeneratorCellService genService = new GeneratorCellService(guiManager, genManager);



        //load all mechanisms
        genManager.loadAllMechanismsFromLoadedChunks();
        barrierManager.loadAllMechanismsFromLoadedChunks();

        //commands
        getCommand("getgen").setExecutor(new GeneratorCommands(this));
        getCommand("givecell").setExecutor(new GeneratorCommands(this));
        getCommand("getbarrier").setExecutor(new GeneratorCommands(this));
        getCommand("getcable").setExecutor(new GeneratorCommands(this));

        //events
        Bukkit.getPluginManager().registerEvents(new GeneratorEvents(this, genManager, guiManager), this);
        Bukkit.getPluginManager().registerEvents(new BarrierEvents(this, barrierManager, databaseManager), this);
        Bukkit.getPluginManager().registerEvents(new CableEvents(this), this);

        LogUtil.info("Плагин загружен!");

        //Сохраняем механизмы каждые 5 минут
        getServer().getScheduler().runTaskTimer(this, () -> {
            genManager.saveAllMechanisms();
            barrierManager.saveAllMechanisms();
        }, 6000L, 6000L); // Каждые 5 минут


        //Синхронизируем передачу внутри инвентаря генератора
        this.tickAllTask = Bukkit.getScheduler().runTaskTimer(
                this,
                () -> {
                    if (guiManager.hasViewers()) {
                        genService.tickOpenGuis();
                    }
                }, 1L, 5L
        );

        //Передаем энергию из ячейки в генератор
        this.tickGuiTask = Bukkit.getScheduler().runTaskTimer(
                this,
                () -> {
                    if (genService.hasActive()) {
                        genService.tickAll();
                    }
                }, 20L, 20L
        );

        //Передаем энергию из генератора барьеру
        energyTicker = new GeneratorBarrierService(this, genManager, barrierManager, cableManager);
        energyTicker.runTaskTimer(this, 20L, 20L);

    }

    @Override
    public void onDisable() {
        //закрываем подключение к БД
        if (databaseManager != null) {
            databaseManager.close();
        }
        //Закрываем задачу передачи энергии из ячейки в генератор
        if (tickAllTask != null) tickAllTask.cancel();
        //Закрываем задачу передачи энергии из ячейки в генератор в инвентаре
        if (tickGuiTask != null) tickGuiTask.cancel();
        //Сохраняем генераторы
        if (genManager != null) {
            genManager.saveAllMechanisms();
        }
        //Сохраняем барьеры
        if(barrierManager != null) {
            barrierManager.saveAllMechanisms();
        }
        getLogger().info("Плагин генераторов выключен!");
    }

    public GeneratorManager getGeneratorManager() {
        return genManager;
    }

    public BarrierManager getBarrierManager() {
        return barrierManager;
    }

    public CableManager getCableManager() {
        return cableManager;
    }

    public static MagicMechanism getInstance() {
        return instance;
    }
}
