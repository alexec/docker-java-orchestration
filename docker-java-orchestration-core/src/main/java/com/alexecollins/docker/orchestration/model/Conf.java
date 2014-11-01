package com.alexecollins.docker.orchestration.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

@SuppressWarnings("CanBeFinal")
public class Conf {
    @JsonProperty(required = false)
    private String tag = null;
    @JsonProperty(required = false)
    private List<Link> links = emptyList();
    @JsonProperty(required = false)
    private Packaging packaging = new Packaging();
    @JsonProperty(required = false)
    private List<String> ports = emptyList();
    @JsonProperty(required = false)
    private List<Id> volumesFrom = emptyList();
    @JsonProperty(required = false)
    private HealthChecks healthChecks = new HealthChecks();
    @JsonProperty(required = false)
    private Map<String, String> env = emptyMap();

    @JsonProperty(required = false)
    private Map<String, String> volumes = emptyMap();


    public boolean hasTag() {
        return !StringUtils.isBlank(tag);
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
