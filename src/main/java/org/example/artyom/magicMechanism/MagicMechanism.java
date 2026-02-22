package org.example.artyom.magicMechanism;

import org.bukkit.Bukkit;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.example.artyom.magicMechanism.commands.GeneratorCommands;
import org.example.artyom.magicMechanism.data.GeneratorGuiManager;
import org.example.artyom.magicMechanism.data.Keys;
import org.example.artyom.magicMechanism.database.DatabaseManager;
import org.example.artyom.magicMechanism.events.BarrierEvents;
import org.example.artyom.magicMechanism.events.GeneratorEvents;
import org.example.artyom.magicMechanism.inventories.barrier.BarrierInventory;
import org.example.artyom.magicMechanism.inventories.generator.GenInventory;
import org.example.artyom.magicMechanism.linkservice.GeneratorBarrierService;
import org.example.artyom.magicMechanism.linkservice.GeneratorCellService;
import org.example.artyom.magicMechanism.utils.LogUtil;

import java.sql.Connection;
import java.sql.SQLException;

public final class MagicMechanism extends JavaPlugin {

    private BukkitTask tickAllTask;
    private BukkitTask tickGuiTask;
    private BukkitTask tickBarrierTask;
    private static MagicMechanism instance;


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
        DatabaseManager db = new DatabaseManager(this, "localhost", 3306, "minecraft", "spigot", password);//"spig0tDBpass!");
        try {
            Connection conn = db.getConnection();
            LogUtil.info("Connected to the database");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        GenInventory baseGenInventory = new GenInventory();
        BarrierInventory barrierInventory = new BarrierInventory();
        GeneratorGuiManager guiManager = new GeneratorGuiManager();
        GeneratorCellService genService = new GeneratorCellService(guiManager, baseGenInventory);
        GeneratorBarrierService genBarrierService = new GeneratorBarrierService(genService.allGenerators());



        getCommand("getgen").setExecutor(new GeneratorCommands());
        getCommand("givecell").setExecutor(new GeneratorCommands());
        getCommand("getbarrier").setExecutor(new GeneratorCommands());

        Bukkit.getPluginManager().registerEvents(new GeneratorEvents(guiManager, genService, baseGenInventory), this);
        Bukkit.getPluginManager().registerEvents(new BarrierEvents(barrierInventory), this);

        this.tickAllTask = Bukkit.getScheduler().runTaskTimer(
                this,
                () -> {
                    if (guiManager.hasViewers()) {
                        genService.tickOpenGuis();
                    }
                }, 1L, 5L
        );

        // 2) Обновление GUI: чаще, но только если нужно
        this.tickGuiTask = Bukkit.getScheduler().runTaskTimer(
                this,
                () -> {
                    if (genService.hasActive()) {
                        genService.tickAll();
                    }
                }, 20L, 20L
        );
        this.tickBarrierTask = Bukkit.getScheduler().runTaskTimer(
                this,
                genBarrierService::tickEnergybarrierGenerator, 1L, 20L
        );

    }

    @Override
    public void onDisable() {
        if (tickAllTask != null) tickAllTask.cancel();
        if (tickGuiTask != null) tickGuiTask.cancel();
        if (tickBarrierTask != null) tickBarrierTask.cancel();
    }

    public static MagicMechanism getInstance() {
        return instance;
    }
}
