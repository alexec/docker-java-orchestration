package com.alexecollins.docker.orchestration.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class HealthChecks {

    private List<Ping> pings = new ArrayList<>();
}
