package org.example.artyom.magicMechanism;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.example.artyom.magicMechanism.commands.GeneratorCommands;
import org.example.artyom.magicMechanism.data.GeneratorGuiManager;
import org.example.artyom.magicMechanism.events.BarrierEvents;
import org.example.artyom.magicMechanism.events.GeneratorEvents;
import org.example.artyom.magicMechanism.service.GeneratorService;

public final class MagicMechanism extends JavaPlugin {

    private BukkitTask tickAllTask;
    private BukkitTask tickGuiTask;
    private static MagicMechanism instance;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        Keys.init(this);
        GeneratorGuiManager guiManager = new GeneratorGuiManager();
        getCommand("getgen").setExecutor(new GeneratorCommands());
        getCommand("givecell").setExecutor(new GeneratorCommands());
        Bukkit.getPluginManager().registerEvents(new GeneratorEvents(guiManager), this);
        Bukkit.getPluginManager().registerEvents(new BarrierEvents(), this);
        GeneratorService genService = new GeneratorService(guiManager);
        this.tickAllTask = Bukkit.getScheduler().runTaskTimer(
                this,
                () -> {
                    if (guiManager.hasViewers()) {   // лучше чем hasActive()
                        genService.tickOpenGuis();
                    }
                },
                1L,
                5L
        );

        // 2) Обновление GUI: чаще, но только если нужно
        this.tickGuiTask = Bukkit.getScheduler().runTaskTimer(
                this,
                () -> {
                    if (genService.hasActive()) {   // лучше чем hasActive()
                        genService.tickAll();
                    }
                },
                20L,
                20L // например, раз в 5 тиков; можешь 20L если достаточно раз в сек
        );
    }

    @Override
    public void onDisable() {
        if (tickAllTask != null) tickAllTask.cancel();
        if (tickGuiTask != null) tickGuiTask.cancel();
    }

    public static MagicMechanism getInstance() {
        return instance;
    }
}
