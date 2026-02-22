package org.example.artyom.magicMechanism.utils;

import org.example.artyom.magicMechanism.MagicMechanism;

import java.sql.SQLException;

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

    public static void error(String msg, SQLException e) {
        plugin.getLogger().severe(msg);
        e.printStackTrace();
    }
}