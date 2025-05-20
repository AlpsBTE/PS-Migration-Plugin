package com.alpsbte.pSMigrationPlugin.core.database;

import com.alpsbte.pSMigrationPlugin.PSMigrationPlugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.kyori.adventure.text.Component.text;

public class DatabaseV1Connection {

    private final static HikariConfig config = new HikariConfig();
    private static HikariDataSource dataSource;

    private static int connectionClosed, connectionOpened;

    public static void InitializeDatabase() throws ClassNotFoundException {
        Class.forName("org.mariadb.jdbc.Driver");

        FileConfiguration configFile = PSMigrationPlugin.getPlugin().getConfig();
        String URL = configFile.getString("database.v1.db-url");
        String name = configFile.getString("database.v1.db-name");
        String username = configFile.getString("database.v1.username");
        String password = configFile.getString("database.v1.password");

        config.setJdbcUrl(URL + name);
        config.setUsername(username);
        config.setPassword(password);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() {
        int retries = 3;
        while (retries > 0) {
            try {
                return dataSource.getConnection();
            } catch (SQLException ex) {
                PSMigrationPlugin.getPlugin().getComponentLogger().error(text("Database connection failed!\n\n" + ex.getMessage()));
            }
            retries--;
        }
        return null;
    }

    public static StatementBuilder createStatement(String sql) {
        return new StatementBuilder(sql);
    }

    public static void closeResultSet(ResultSet resultSet) throws SQLException {
        if (resultSet.isClosed()
                && resultSet.getStatement().isClosed()
                && resultSet.getStatement().getConnection().isClosed())
            return;

        resultSet.close();
        resultSet.getStatement().close();
        resultSet.getStatement().getConnection().close();

        connectionClosed++;

        if (connectionOpened > connectionClosed + 5)
            PSMigrationPlugin.getPlugin().getComponentLogger().error(text("There are multiple database connections opened. Please report this issue."));
    }

    public static class StatementBuilder {
        private final String sql;
        private final List<Object> values = new ArrayList<>();

        public StatementBuilder(String sql) {
            this.sql = sql;
        }

        public StatementBuilder setValue(Object value) {
            values.add(value);
            return this;
        }

        public ResultSet executeQuery() throws SQLException {
            Connection con = dataSource.getConnection();
            PreparedStatement ps = Objects.requireNonNull(con).prepareStatement(sql);
            ResultSet rs = iterateValues(ps).executeQuery();

            connectionOpened++;

            return rs;
        }

        public void executeUpdate() throws SQLException {
            try (Connection con = dataSource.getConnection()) {
                try (PreparedStatement ps = Objects.requireNonNull(con).prepareStatement(sql)) {
                    iterateValues(ps).executeUpdate();
                }
            }
        }

        private PreparedStatement iterateValues(PreparedStatement ps) throws SQLException {
            for (int i = 0; i < values.size(); i++) {
                ps.setObject(i + 1, values.get(i));
            }
            return ps;
        }
    }
}

