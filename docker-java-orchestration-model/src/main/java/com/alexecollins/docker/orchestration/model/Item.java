package com.alexecollins.docker.orchestration.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Item {
    @JsonProperty(required = true)
    private String path;
    @JsonProperty
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
