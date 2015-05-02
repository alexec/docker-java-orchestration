package com.alexecollins.docker.orchestration.model;

import lombok.Data;

@Data
public class Item {
    private String path;
    private boolean filter = true;

    public Item() {
    }

    // allows for backwards compatibility
    public Item(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public boolean shouldFilter() {
        return filter;
    }
}
