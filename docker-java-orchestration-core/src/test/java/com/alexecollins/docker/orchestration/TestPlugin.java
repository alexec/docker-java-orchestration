package com.alexecollins.docker.orchestration;

import com.alexecollins.docker.orchestration.model.Conf;
import com.alexecollins.docker.orchestration.model.Id;
import com.alexecollins.docker.orchestration.plugin.api.Plugin;

public class TestPlugin implements Plugin {
    private Id lastStarted = null;

    Id lastStarted() {
        return lastStarted;
    }

    @Override
    public void started(Id id, Conf conf) {
        lastStarted = id;
    }
}
