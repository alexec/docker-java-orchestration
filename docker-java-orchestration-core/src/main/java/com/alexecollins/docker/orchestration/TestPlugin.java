package com.alexecollins.docker.orchestration;

import com.alexecollins.docker.orchestration.plugin.api.Plugin;

public class TestPlugin implements Plugin {
    private String started = null;

    public String lastStarted() {
        return started;
    }

    @Override
    public void started(String id) {
        started = id;
    }
}
