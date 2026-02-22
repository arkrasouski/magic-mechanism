package org.example.artyom.magicMechanism.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.example.artyom.magicMechanism.utils.LogUtil;

import java.sql.*;



public class DatabaseManager {
    private HikariDataSource dataSource;

    public DatabaseManager( String host, int port, String database, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + database);
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName("org.mariadb.jdbc.Driver");
        // Настройки пула
        config.setMaximumPoolSize(10);          // максимум соединений в пуле
        config.setMinimumIdle(5);                // минимум idle соединений
        config.setConnectionTimeout(30000);      // таймаут получения соединения (мс)
        config.setIdleTimeout(600000);           // таймаут idle соединения (мс)
        config.setMaxLifetime(1800000);          // макс. время жизни соединения (мс)

        // Дополнительные параметры
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);
        LogUtil.info("Пул соединений HikariCP инициализирован");
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();  // получаем соединение из пула
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();  // закрываем все соединения при выключении
        }
    }
}
