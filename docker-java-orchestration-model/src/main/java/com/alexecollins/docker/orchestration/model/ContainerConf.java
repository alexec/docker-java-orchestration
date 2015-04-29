package com.alexecollins.docker.orchestration.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ContainerConf {
    @JsonProperty
    private String name;

    public boolean hasName() {
        return name != null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
