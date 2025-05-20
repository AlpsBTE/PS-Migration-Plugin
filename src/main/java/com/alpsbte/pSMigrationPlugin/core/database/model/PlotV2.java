package com.alpsbte.pSMigrationPlugin.core.database.model;

public class PlotV2 {
    private final int id;
    private final Status status;

    public PlotV2(int id, Status status) {
        this.id = id;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public Status getStatus() {
        return status;
    }
}
