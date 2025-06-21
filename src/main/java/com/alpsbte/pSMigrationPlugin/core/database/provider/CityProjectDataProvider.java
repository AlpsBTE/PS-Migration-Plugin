package com.alpsbte.pSMigrationPlugin.core.database.provider;

import com.alpsbte.pSMigrationPlugin.PSMigrationPlugin;
import com.alpsbte.pSMigrationPlugin.core.database.DatabaseV1Connection;
import com.alpsbte.pSMigrationPlugin.core.database.model.CityProjectV1;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CityProjectDataProvider {
    private CityProjectDataProvider() {
    }

    public static List<CityProjectV1> getAllCityProjects() {
        List<CityProjectV1> cities = new ArrayList<>();
        try (ResultSet rs = DatabaseV1Connection.createStatement("SELECT id, name, description FROM plotsystem_city_projects").executeQuery()){
            while (rs.next()) {
                int id = rs.getInt(1);
                String name = rs.getString(2);
                String description = rs.getString(3);

                cities.add(new CityProjectV1(id, name, description));
            }
        } catch (SQLException e) {
            PSMigrationPlugin.getPlugin().getComponentLogger().error("SQL Error occurred!", e);
        }

        return cities;
    }
}
