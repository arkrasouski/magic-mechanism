package org.example.artyom.magicMechanism.database;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DatabaseManager {
    private final JavaPlugin plugin;
    private final String url;
    private final String user;
    private final String password;

    // Простой пул из 5 соединений
    private final BlockingQueue<Connection> connectionPool = new LinkedBlockingQueue<>(5);

    public DatabaseManager(JavaPlugin plugin, String host, int port, String db,
                           String user, String pass) {
        this.plugin = plugin;
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("MariaDB Driver not found!");
        }

        this.url = "jdbc:mariadb://" + host + ":" + port + "/" + db + "?useSSL=false&autoReconnect=true";
        this.user = user;
        this.password = pass;


        initPool();
    }

    private void initPool() {
        for (int i = 0; i < 5; i++) {
            try {
                connectionPool.offer(createConnection());
            } catch (SQLException e) {
                plugin.getLogger().severe("Не удалось создать соединение #" + i);
            }
        }
    }

    private Connection createConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public Connection getConnection() throws SQLException {
        try {
            // Берем из пула или ждем 5 сек
            Connection conn = connectionPool.poll(5, TimeUnit.SECONDS);
            if (conn == null || conn.isClosed()) {
                return createConnection();
            }
            return conn;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Прерывание получения соединения");
        }
    }

    public void releaseConnection(Connection conn) throws SQLException {
        if (conn != null && !conn.isClosed()) {
            try {
                connectionPool.offer(conn);
            } catch (Exception e) {
                try { conn.close(); } catch (Exception ignored) {}
            }
        }
    }


    public void close() {
        connectionPool.clear();
    }
}
