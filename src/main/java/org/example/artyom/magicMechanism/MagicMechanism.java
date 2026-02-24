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
import org.example.artyom.magicMechanism.events.GeneratorEvents;
import org.example.artyom.magicMechanism.inventories.barrier.BarrierInventory;
import org.example.artyom.magicMechanism.inventories.generator.GenInventory;
//import org.example.artyom.magicMechanism.linkservice.GeneratorBarrierService;
//import org.example.artyom.magicMechanism.linkservice.GeneratorCellService;
import org.example.artyom.magicMechanism.linkservice.GeneratorBarrierService;
import org.example.artyom.magicMechanism.linkservice.GeneratorCellService;
import org.example.artyom.magicMechanism.managers.BarrierManager;
import org.example.artyom.magicMechanism.managers.GeneratorManager;
import org.example.artyom.magicMechanism.mechanisms.BaseMechanism;
import org.example.artyom.magicMechanism.mechanisms.Generator;
import org.example.artyom.magicMechanism.utils.LogUtil;

import java.sql.Connection;
import java.sql.SQLException;

public final class MagicMechanism extends JavaPlugin {

    private BukkitTask tickAllTask;
    private BukkitTask tickGuiTask;
    private BukkitTask tickBarrierTask;
    private static MagicMechanism instance;
    private DatabaseManager databaseManager;
    private GeneratorBarrierService energyTicker;
    private GeneratorManager genManager;
    @Override
    public void onEnable() {

        // Сохраняем конфиг по умолчанию из resources
        saveDefaultConfig();
        // Перезагружаем конфиг (на всякий случай)
        reloadConfig();
        // Plugin startup logic
        instance = this;
        Keys.init(this);
        LogUtil.init(this);
        LogUtil.info("Плагин загружен!");
        String password = getConfig().getString("database.password",
                System.getenv("MYSQL_PASSWORD") != null ? System.getenv("MYSQL_PASSWORD") : "default");
        getLogger().info("Attempting to connect with password: " + password);
        databaseManager = new DatabaseManager("localhost", 3306, "minecraft", "spigot", "spig0tDBpass!");//password);//"spig0tDBpass!");
        genManager = new GeneratorManager(this);
        GeneratorGuiManager guiManager = new GeneratorGuiManager();
        GeneratorCellService genService = new GeneratorCellService(guiManager, genManager);
        BarrierManager barrierManager = new BarrierManager(this);
        GeneratorBarrierService genBarrierService = new GeneratorBarrierService(this, genManager, barrierManager);
        genManager.loadAllMechanismsFromLoadedChunks();

        getServer().getScheduler().runTaskTimer(this, () -> {
            genManager.saveAllMechanisms();
        }, 6000L, 6000L); // Каждые 5 минут

        getCommand("getgen").setExecutor(new GeneratorCommands(this));
        getCommand("givecell").setExecutor(new GeneratorCommands(this));
        getCommand("getbarrier").setExecutor(new GeneratorCommands(this));

        Bukkit.getPluginManager().registerEvents(new GeneratorEvents(this, genManager, guiManager), this);
        Bukkit.getPluginManager().registerEvents(new BarrierEvents(this, barrierManager, databaseManager), this);

        this.tickAllTask = Bukkit.getScheduler().runTaskTimer(
                this,
                () -> {
                    if (guiManager.hasViewers()) {
                        genService.tickOpenGuis();
                    }
                }, 1L, 5L
        );
//
//        // 2) Обновление GUI: чаще, но только если нужно
        this.tickGuiTask = Bukkit.getScheduler().runTaskTimer(
                this,
                () -> {
                    if (genService.hasActive()) {
                        genService.tickAll();
                    }
                }, 20L, 20L
        );
        energyTicker = new GeneratorBarrierService(this, genManager, barrierManager);
        energyTicker.runTaskTimer(this, 20L, 20L);

    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        if (tickAllTask != null) tickAllTask.cancel();
        if (tickGuiTask != null) tickGuiTask.cancel();
        if (tickBarrierTask != null) tickBarrierTask.cancel();
        if (genManager != null) {
            genManager.saveAllMechanisms();
        }

        getLogger().info("Плагин генераторов выключен!");
    }


    public static MagicMechanism getInstance() {
        return instance;
    }
}
