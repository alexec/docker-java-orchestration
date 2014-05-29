package com.alexecollins.docker.orchestration.model;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import static java.util.Collections.emptyList;

@SuppressWarnings("CanBeFinal")
public class Conf {
    @JsonProperty(required = false)
    private List<Id> links = emptyList();
    @JsonProperty(required = false)
    private Packaging packaging = new Packaging();
    @JsonProperty(required = false)
    private List<String> ports = emptyList();
    @JsonProperty(required = false)
    private List<Id> volumesFrom = emptyList();
	@JsonProperty(required = false)
	private HealthChecks healthChecks = new HealthChecks();

    public List<Id> getLinks() {
        return links;
    }

    public List<String> getPorts() {
        return ports;
    }

	public HealthChecks getHealthChecks() {
		return healthChecks;
	}

	public List<Id> getVolumesFrom() {
		return volumesFrom;
	}

	public Packaging getPackaging() {
		return packaging;
	}
}
