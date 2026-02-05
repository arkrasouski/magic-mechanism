package org.example.artyom.magicMechanism;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.example.artyom.magicMechanism.commands.GeneratorCommands;
import org.example.artyom.magicMechanism.events.BarrierEvents;
import org.example.artyom.magicMechanism.events.GeneratorEvents;
import org.example.artyom.magicMechanism.service.GeneratorService;

public final class MagicMechanism extends JavaPlugin {

    private static MagicMechanism instance;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        Keys.init(this);
        getCommand("getgen").setExecutor(new GeneratorCommands());
        getCommand("givecell").setExecutor(new GeneratorCommands());
        Bukkit.getPluginManager().registerEvents(new GeneratorEvents(), this);
        Bukkit.getPluginManager().registerEvents(new BarrierEvents(), this);

        new BukkitRunnable() {
            @Override public void run() {
                GeneratorService.tickAll();
            }
        }.runTaskTimer(this, 1L, 20L); // period == между повторами, delay == стартовая задержка
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static MagicMechanism getInstance() {
        return instance;
    }
}
