package org.example.artyom.magicMechanism.managers;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.example.artyom.magicMechanism.data.interfaces.IEnergyHandler;
import org.example.artyom.magicMechanism.network.EnergyNetwork;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EnergyManager{
    private static Map<Location, IEnergyHandler> handlers = new HashMap<>();

    public static void registerHandler(Location loc, IEnergyHandler handler) {
        handlers.put(loc, handler);
    }

    public static void removeHandler(Location loc) {
        handlers.remove(loc);
    }

    public static IEnergyHandler getHandler(Location loc) {
        return handlers.get(loc);
    }

    public static Map<Location, IEnergyHandler> getAllHandlers() {
        return Collections.unmodifiableMap(handlers);
    }

}
