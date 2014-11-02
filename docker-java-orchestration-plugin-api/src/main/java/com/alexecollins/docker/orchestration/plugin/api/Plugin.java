package com.alexecollins.docker.orchestration.plugin.api;

import com.alexecollins.docker.orchestration.model.Conf;
import com.alexecollins.docker.orchestration.model.Id;

public interface Plugin {
    /**
     * Called when a container is started.
     * @param id The id of the container, not null.
     * @param conf The conf of the container, not null.
     */
    void started(Id id, Conf conf);
}
