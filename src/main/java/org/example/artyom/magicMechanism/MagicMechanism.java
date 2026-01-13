package org.example.artyom.magicMechanism;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.example.artyom.magicMechanism.commands.GeneratorCommands;
import org.example.artyom.magicMechanism.events.GeneratorEvents;

public final class MagicMechanism extends JavaPlugin {

    private static MagicMechanism instance;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;

        getCommand("getgen").setExecutor(new GeneratorCommands());
        Bukkit.getPluginManager().registerEvents(new GeneratorEvents(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static MagicMechanism getInstance() {
        return instance;
    }
}
