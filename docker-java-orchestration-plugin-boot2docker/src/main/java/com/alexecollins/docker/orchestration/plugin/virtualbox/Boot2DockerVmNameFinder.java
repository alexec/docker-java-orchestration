package com.alexecollins.docker.orchestration.plugin.virtualbox;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class Boot2DockerVmNameFinder {
    private final VirtualBoxFacade virtualBoxFacade;

    Boot2DockerVmNameFinder(VirtualBoxFacade virtualBoxFacade) {
        this.virtualBoxFacade = virtualBoxFacade;
    }

    String getVmName() {
        List<String> vmNames = virtualBoxFacade.getVmNames();
        Collections.sort(vmNames);
        for (String vmName : vmNames) {
            if (Arrays.asList("boot2docker-vm", "default").contains(vmName)) {
                return vmName;
            }
        }
        throw new IllegalStateException("unable to determine VM name from " + vmNames);
    }


}
