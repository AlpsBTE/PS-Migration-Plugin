package com.alpsbte.pSMigrationPlugin.core.database.provider;

import com.alpsbte.pSMigrationPlugin.PSMigrationPlugin;
import com.alpsbte.pSMigrationPlugin.core.database.DatabaseV1Connection;
import com.alpsbte.pSMigrationPlugin.core.database.DatabaseV2Connection;
import com.alpsbte.pSMigrationPlugin.core.database.model.PlotV1;
import com.alpsbte.pSMigrationPlugin.core.database.model.PlotV2;
import com.alpsbte.pSMigrationPlugin.core.database.model.Status;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlotDataProvider {
    private PlotDataProvider() {
    }

    public static Optional<PlotV1> getOldPlot(int id) {
        try (@NotNull Connection connection = DatabaseV1Connection.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT p.id, c.server_id, p.city_project_id FROM plotsystem_plots p" +
                    " INNER JOIN plotsystem_city_projects ct ON p.city_project_id=ct.id" +
                    " INNER JOIN plotsystem_countries c ON ct.country_id=c.id" +
                    " WHERE p.id=?")) {
                preparedStatement.setInt(1, id);
                try (ResultSet rs = preparedStatement.executeQuery()) {
                    if (!rs.next()) return Optional.empty();

                    int serverId = rs.getInt(2);
                    int cityProjectId = rs.getInt(3);

                    return Optional.of(new PlotV1(id, serverId, cityProjectId));
                }
            }
        } catch (SQLException e) {
            PSMigrationPlugin.getPlugin().getComponentLogger().error("SQL Error occurred!", e);
        }

        return Optional.empty();
    }

    public static List<PlotV2> getMigrationPlots() {
        List<PlotV2> plots = new ArrayList<>();
        try (@NotNull Connection connection = DatabaseV2Connection.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT plot_id, status FROM plot WHERE plot_version = 3")) {
                try (ResultSet rs = preparedStatement.executeQuery()) {
                    while (rs.next()) {
                        int id = rs.getInt(1);
                        Status status = Status.valueOf(rs.getString(2).toUpperCase());
                        PlotV2 plot = new PlotV2(id, status);
                        plots.add(plot);
                    }
                }
            }
        } catch (SQLException e) {
            PSMigrationPlugin.getPlugin().getComponentLogger().error("SQL Error occurred!", e);
        }
        return plots;
    }

    public static void updatePlot(PlotV2 plot, String mcVersion, byte[] initialSchematic, byte[] completedSchematic) {

        try (@NotNull Connection connection = DatabaseV2Connection.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE plot SET mc_version=?, initial_schematic=?, complete_schematic=?, plot_version=4 WHERE plot_id=?")) {
                preparedStatement.setString(1, mcVersion);
                preparedStatement.setBytes(2, initialSchematic);
                preparedStatement.setBytes(3, completedSchematic);
                preparedStatement.setInt(4, plot.getId());
                try (ResultSet rs = preparedStatement.executeQuery()) {
                }
            }
        } catch (SQLException e) {
            PSMigrationPlugin.getPlugin().getComponentLogger().error("SQL Error occurred!", e);
        }
    }
}
