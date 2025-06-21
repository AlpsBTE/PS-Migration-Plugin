package com.alpsbte.pSMigrationPlugin.core.database.model;

public class CityProjectV1 {
    private final int id;
    private final String name;
    private final String description;

    public CityProjectV1(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
