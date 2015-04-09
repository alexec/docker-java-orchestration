package com.alexecollins.docker.orchestration.model;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("CanBeFinal")
public class Conf {
    @JsonProperty
    private List<String> tags = new ArrayList<String>();
    @JsonProperty
    private List<Link> links = new ArrayList<Link>();
    @JsonProperty
    private Packaging packaging = new Packaging();
    /**
     * E.g. "8080" or "8080 8080" where the former is the exposed port and the latter the container port.
     */
    @JsonProperty
    private List<String> ports = new ArrayList<String>();
    @JsonProperty
    private int sleep = 0;
    @JsonProperty
    private List<Id> volumesFrom = new ArrayList<Id>();
    @JsonProperty
    private HealthChecks healthChecks = new HealthChecks();
    @JsonProperty
    private Map<String, String> env = new HashMap<String, String>();

    @JsonProperty
    private Map<String, String> volumes = new HashMap<String, String>();

    @JsonProperty
    private boolean exposeContainerIp = true;

    public boolean isExposeContainerIp() {
        return exposeContainerIp;
    }

    public boolean hasTag() {
      return tags != null && !tags.isEmpty();
    }

    /**
     * @return Returns the first tag of a list of tags
     */
    public String getTag() {
        return tags.get(0);
    }

    public void setTag(String tag) {
        setTags(Arrays.asList(tag));
    }
    
    public List<String> getTags() {
      return tags;
    }
    
    public void setTags(List<String> tags) {
      this.tags = tags;
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

    public int getSleep() {
        return sleep;
    }

    public void setSleep(int sleep) {
        this.sleep = sleep;
    }
}
