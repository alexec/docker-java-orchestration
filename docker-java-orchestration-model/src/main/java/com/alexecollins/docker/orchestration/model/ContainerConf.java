package com.alexecollins.docker.orchestration.model;

import lombok.Data;

@Data
public class ContainerConf {

    private String name;

    public boolean hasName() {
        return name != null;
    }
}
