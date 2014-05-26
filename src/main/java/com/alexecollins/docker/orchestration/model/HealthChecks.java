package com.alexecollins.docker.orchestration.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import static java.util.Collections.emptyList;

public class HealthChecks {
	@JsonProperty(required = false)
	public List<Ping> pings = emptyList();
}
