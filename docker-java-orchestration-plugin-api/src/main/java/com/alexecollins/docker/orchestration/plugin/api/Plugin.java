package com.alexecollins.docker.orchestration.plugin.api;

public interface Plugin {
    /**
     * Called when a container is started.
     * @param id The ID of the container, not null.
     */
    void started(String id);
}
