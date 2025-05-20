package com.alpsbte.pSMigrationPlugin.core.database.model;

public class PlotV1 {
    private final int id;
    private final int serverId;
    private final int cityProjectId;

    public PlotV1(int id, int serverId, int cityProjectId) {
        this.id = id;
        this.serverId = serverId;
        this.cityProjectId = cityProjectId;
    }

    public int getId() {
        return id;
    }

    public int getServerId() {
        return serverId;
    }

    public int getCityProjectId() {
        return cityProjectId;
    }
}
