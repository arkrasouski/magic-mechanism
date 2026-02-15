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
import org.example.artyom.magicMechanism.linkservice.GeneratorCellService;
import org.example.artyom.magicMechanism.mechanisms.Generator;

import java.util.Collection;
import java.util.Map;

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
        GeneratorCellService genService = new GeneratorCellService(guiManager);

        getCommand("getgen").setExecutor(new GeneratorCommands());
        getCommand("givecell").setExecutor(new GeneratorCommands());
        getCommand("getbarrier").setExecutor(new GeneratorCommands());

        Bukkit.getPluginManager().registerEvents(new GeneratorEvents(guiManager, genService), this);
        Bukkit.getPluginManager().registerEvents(new BarrierEvents(), this);

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

        new BukkitRunnable() {
            @Override public void run() {

                Collection<Map.Entry<BlockPosKey, Generator>> generators = genService.allGenerators();

                for (Map.Entry<BlockPosKey, Generator> entry : generators) { // [web:72]
                    BlockPosKey key = entry.getKey();            // [web:72]
                    Generator gen = entry.getValue();        // [web:72]

                    Block genBlock = BlockPosKey.blockFromKey(key); // или восстанови Block из key (world+x+y+z)
                    BlockState bs = genBlock.getState();
                    if(!(bs instanceof TileState tileGen)) return;
                    int genEnergy = gen.getCurrentEnergy(tileGen);

                    for (Block barrier : Generator.adjacentMechanisms(genBlock)) {

//
                        if (genEnergy <= 0) break;

                        BlockState barrierState = barrier.getState();
                        if(!(barrierState instanceof TileState tile)) continue;

                        PersistentDataContainer pdc =  tile.getPersistentDataContainer();
                        int buf = pdc.getOrDefault(Keys.BUFFER, PersistentDataType.INTEGER, 0);

                        int moved = Math.min(gen.getFrequency(), genEnergy);
                        moved = Math.min(moved, Generator.capacity - buf);
                        if (moved <= 0) continue;

                        // Меняем ТОЛЬКО здесь
                        genEnergy -= moved;
                        gen.setCurrentEnergy(tileGen, genEnergy);
                        //EnergyCell.setEnergy(cell, cellEnergy - moved); // внутри обновится lore

                        pdc.set(Keys.BUFFER, PersistentDataType.INTEGER, buf + moved);

                        tile.update();

                        //GenStorage.saveItems(tile, inv);
                    }
                    tileGen.update();

                }}
        }.runTaskTimer(this, 1L, 20L);

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
