package com.alexecollins.docker.orchestration.model;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("CanBeFinal")
public class Conf {
    @JsonProperty(required = false)
    private String tag = null;
    @JsonProperty(required = false)
    private List<Link> links = new ArrayList<Link>();
    @JsonProperty(required = false)
    private Packaging packaging = new Packaging();
    /**
     * E.g. "8080" or "8080 8080" where the former is the exposed port and the latter the container port.
     */
    @JsonProperty(required = false)
    private List<String> ports = new ArrayList<String>();
    @JsonProperty(required = false)
    private List<Id> volumesFrom = new ArrayList<Id>();
    @JsonProperty(required = false)
    private HealthChecks healthChecks = new HealthChecks();
    @JsonProperty(required = false)
    private Map<String, String> env = new HashMap<String, String>();

    @JsonProperty(required = false)
    private Map<String, String> volumes = new HashMap<String, String>();


    public boolean hasTag() {
        return tag != null && !tag.isEmpty();
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public List<Link> getLinks() {
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

    public Map<String, String> getEnv() {
        return this.env;
    }

    public Map<String, String> getVolumes() {
        return volumes;
    }
}
