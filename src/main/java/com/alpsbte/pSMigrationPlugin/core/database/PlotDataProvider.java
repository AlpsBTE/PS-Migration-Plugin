package com.alpsbte.pSMigrationPlugin.core.database;

import com.alpsbte.pSMigrationPlugin.PSMigrationPlugin;
import com.alpsbte.pSMigrationPlugin.core.database.model.PlotV1;
import com.alpsbte.pSMigrationPlugin.core.database.model.PlotV2;
import com.alpsbte.pSMigrationPlugin.core.database.model.Status;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlotDataProvider {
    public static Optional<PlotV1> getOldPlot(int id) {
        try (ResultSet rs = DatabaseV1Connection.createStatement("SELECT p.id, c.server_id, p.city_project_id FROM plotsystem_plots p" +
                        " INNER JOIN plotsystem_city_projects ct ON p.city_project_id=ct.id" +
                        " INNER JOIN plotsystem_countries c ON ct.country_id=c.id" +
                        " WHERE p.version = 3 AND p.id=?")
                .setValue(id).executeQuery()) {
            if (!rs.next()) return Optional.empty();

            int serverId = rs.getInt(2);
            int cityProjectId = rs.getInt(3);

            DatabaseV1Connection.closeResultSet(rs);
            return Optional.of(new PlotV1(id, serverId, cityProjectId));
        } catch (SQLException ex) {
            PSMigrationPlugin.getPlugin().getComponentLogger().error("SQL Error occurred!", ex);
        }
        return Optional.empty();
    }

    public static List<PlotV2> getMigrationPlots() {
        List<PlotV2> plots = new ArrayList<>();
        try (ResultSet rs = DatabaseV2Connection.createStatement("SELECT plot_id, status FROM plot WHERE plot_version = 3").executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt(1);
                Status status = Status.valueOf(rs.getString(2).toUpperCase());
                PlotV2 plot = new PlotV2(id, status);
                plots.add(plot);
            }

            DatabaseV2Connection.closeResultSet(rs);
        } catch (SQLException ex) {
            PSMigrationPlugin.getPlugin().getComponentLogger().error("SQL Error occurred!", ex);
        }
        return plots;
    }

    public static void updatePlot(PlotV2 plot, String mcVersion, byte[] initialSchematic, byte[] completedSchematic) {
        try {
            DatabaseV2Connection.createStatement("UPDATE plot SET mc_version=?, initial_schematic=?, complete_schematic=?, plot_version=4 WHERE plot_id=?")
                    .setValue(mcVersion)
                    .setValue(initialSchematic)
                    .setValue(completedSchematic)
                    .setValue(plot.getId()).executeUpdate();
        } catch (SQLException ex) {
            PSMigrationPlugin.getPlugin().getComponentLogger().error("SQL Error occurred!", ex);
        }
    }
}
