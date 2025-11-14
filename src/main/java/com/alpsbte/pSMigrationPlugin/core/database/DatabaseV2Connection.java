package com.alpsbte.pSMigrationPlugin.core.database;

import com.alpsbte.pSMigrationPlugin.PSMigrationPlugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseV2Connection {
    private DatabaseV2Connection() {
    }

    private static HikariDataSource hikari;
    private static Logger logger;

    /**
     * Initializes the connection pool with the given configuration data.
     *
     */
    public static void initializeDatabase() throws ClassNotFoundException {
        FileConfiguration configFile = PSMigrationPlugin.getPlugin().getConfig();
        String URL = configFile.getString("database.v2.db-url");
        String name = configFile.getString("database.v2.db-name");
        String username = configFile.getString("database.v2.username");
        String password = configFile.getString("database.v2.password");

        Class.forName("org.mariadb.jdbc.Driver");
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(URL + name + "?allowMultiQueries=true");
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");

        hikari = new HikariDataSource(hikariConfig);
        logger = LoggerFactory.getLogger(DatabaseV1Connection.class);

    }

    /**
     * Returns a new connection from the connection pool.
     *
     * @return An open SQL connection
     * @throws SQLException If no connection is available
     */
    public static @NotNull Connection getConnection() throws SQLException {
        if (hikari == null) {
            throw new SQLException("Unable to get a connection from the pool. (hikari is null)");
        }

        Connection connection = hikari.getConnection();
        if (connection == null) {
            throw new SQLException("Unable to get a connection from the pool. (getConnection returned null)");
        }

        return connection;
    }

    /**
     * Closes the connection pool and releases all resources.
     * Logs success or a warning if no connection exists, if logger is set.
     */
    public static void shutdown() {
        if (hikari != null) {
            hikari.close();
            if (logger != null) logger.info("Database connection closed successfully.");
        } else {
            if (logger != null) logger.warn("No database connection to close.");
        }
    }
}
