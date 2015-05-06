package com.alexecollins.docker.orchestration.plugin.virtualbox;

import com.alexecollins.docker.orchestration.model.Conf;
import com.alexecollins.docker.orchestration.model.Id;
import com.alexecollins.docker.orchestration.plugin.api.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Boot2DockerPlugin implements Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(Boot2DockerPlugin.class);
    private final boolean skip = isDisabled();
    private final VirtualBoxFacade virtualBoxFacade = new VirtualBoxFacade();
    private final String skipReason = "host is Unix like";


    private static int hostPort(String stringPort) {
        return Integer.parseInt(stringPort.split(" ")[0]);
    }

    @Override
    public void started(Id id, Conf conf) {
        if (skip) {
            LOGGER.info("Skipping Boot2Docker set-up because " + skipReason);
            return;
        }
        for (String stringPort : conf.getPorts()) {
            virtualBoxFacade.recreatePortForward(hostPort(stringPort));
        }
    }

    @Override
    public void stopped(Id id, Conf conf) {
        if (skip) {
            LOGGER.info("Skipping Boot2Docker tear-down because " + skipReason);
            return;
        }
        for (String stringPort : conf.getPorts()) {
            virtualBoxFacade.deletePortForward(hostPort(stringPort));
        }
    }

    private boolean isDisabled() {
        return !OS.isNotUnix();
    }
}
