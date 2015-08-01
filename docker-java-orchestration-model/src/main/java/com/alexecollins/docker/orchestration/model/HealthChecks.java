package com.alexecollins.docker.orchestration.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Data
public class HealthChecks {

    private List<Ping> pings = new ArrayList<>();
    private List<LogPattern> logPatterns = new ArrayList<>();
}
