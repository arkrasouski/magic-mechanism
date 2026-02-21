package org.example.artyom.magicMechanism.utils;

import org.example.artyom.magicMechanism.MagicMechanism;

public class LogUtil {
    private static MagicMechanism plugin;

    public static void init(MagicMechanism pluginInstance) {
        plugin = pluginInstance;
    }

    public static void info(String msg) {
        plugin.getLogger().info(msg);
    }

    public static void warn(String msg) {
        plugin.getLogger().warning(msg);
    }
}