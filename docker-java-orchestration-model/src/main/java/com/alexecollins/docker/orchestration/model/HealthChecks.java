package com.alexecollins.docker.orchestration.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class HealthChecks {
    @JsonProperty
    private List<Ping> pings = new ArrayList<Ping>();

    public List<Ping> getPings() {
        return pings;
    }
}
