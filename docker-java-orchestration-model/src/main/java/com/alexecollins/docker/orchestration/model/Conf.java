package com.alexecollins.docker.orchestration.model;


import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class Conf {

    private List<String> tags = new ArrayList<>();
    /**
     * Information about the container. This is intended to provide a place for container only properties.
     */
    private ContainerConf container = new ContainerConf();
    private List<Link> links = new ArrayList<>();
    private Packaging packaging = new Packaging();
    /**
     * E.g. "8080" or "8080 8080" where the former is the exposed port and the latter the container port.
     */
    private List<String> ports = new ArrayList<>();
    private int sleep = 1000;
    /**
     * @deprecated We always log both success and failure.
     */
    @Deprecated
    private boolean logOnFailure = true;
    private int maxLogLines = 10; // same as unix tail command
    private List<VolumeFrom> volumesFrom = new ArrayList<>();
    private HealthChecks healthChecks = new HealthChecks();
    private Map<String, String> env = new HashMap<>();
    private Map<String, String> volumes = new HashMap<>();
    private boolean enabled = true;
    private boolean exposeContainerIp = true;
    private List<String> extraHosts = new ArrayList<>();
    private boolean privileged;
    private String networkMode = "bridge";

    public Conf() {
    }

    @SuppressWarnings("UnusedParameters")
    public Conf(String ignored) {
        if (!ignored.isEmpty()) {
            throw new IllegalArgumentException();
        }
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
        setTags(Collections.singletonList(tag));
    }
}
