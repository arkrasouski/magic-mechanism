package org.example.artyom.magicMechanism;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.example.artyom.magicMechanism.commands.GeneratorCommands;
import org.example.artyom.magicMechanism.data.records.BlockPosKey;
import org.example.artyom.magicMechanism.data.GeneratorGuiManager;
import org.example.artyom.magicMechanism.data.Keys;
import org.example.artyom.magicMechanism.events.BarrierEvents;
import org.example.artyom.magicMechanism.events.GeneratorEvents;
import org.example.artyom.magicMechanism.inventories.FillGenInventory;
import org.example.artyom.magicMechanism.linkservice.GeneratorBarrierService;
import org.example.artyom.magicMechanism.linkservice.GeneratorCellService;
import org.example.artyom.magicMechanism.mechanisms.Generator;

import java.util.Collection;
import java.util.Map;

public final class MagicMechanism extends JavaPlugin {

    private BukkitTask tickAllTask;
    private BukkitTask tickGuiTask;
    private BukkitTask tickBarrierTask;
    private static MagicMechanism instance;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        Keys.init(this);
        FillGenInventory baseGenInventory = new FillGenInventory();
        GeneratorGuiManager guiManager = new GeneratorGuiManager();
        GeneratorCellService genService = new GeneratorCellService(guiManager, baseGenInventory);
        GeneratorBarrierService genBarrierService = new GeneratorBarrierService(genService.allGenerators());



        getCommand("getgen").setExecutor(new GeneratorCommands());
        getCommand("givecell").setExecutor(new GeneratorCommands());
        getCommand("getbarrier").setExecutor(new GeneratorCommands());

        Bukkit.getPluginManager().registerEvents(new GeneratorEvents(guiManager, genService, baseGenInventory), this);
        Bukkit.getPluginManager().registerEvents(new BarrierEvents(), this);

        this.tickAllTask = Bukkit.getScheduler().runTaskTimer(
                this,
                () -> {
                    if (guiManager.hasViewers()) {   // лучше чем hasActive()
                        genService.tickOpenGuis();
                    }
                }, 1L, 5L
        );

        // 2) Обновление GUI: чаще, но только если нужно
        this.tickGuiTask = Bukkit.getScheduler().runTaskTimer(
                this,
                () -> {
                    if (genService.hasActive()) {   // лучше чем hasActive()
                        genService.tickAll();
                    }
                }, 20L, 20L // например, раз в 5 тиков; можешь 20L если достаточно раз в сек
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
