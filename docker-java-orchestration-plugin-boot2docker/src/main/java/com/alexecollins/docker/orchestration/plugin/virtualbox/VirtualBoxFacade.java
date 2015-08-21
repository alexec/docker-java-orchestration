package com.alexecollins.docker.orchestration.plugin.virtualbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class VirtualBoxFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualBoxFacade.class);
    private final CommandExecutor commandExecutor;

    VirtualBoxFacade() {
        this(new CommandExecutor());
    }

    VirtualBoxFacade(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }


    void deletePortForward(String vmName, int port) {
        if (isPortForwarded(vmName, port)) {
            LOGGER.info("Deleting VirtualBox port forward for " + port);
            commandExecutor.exec(String.format("VBoxManage controlvm " + vmName + " natpf1 delete %d", port));
        }
    }

    void createPortForward(String vmName, int port) {
        if (!isPortForwarded(vmName, port)) {
            LOGGER.info("Creating VirtualBox port forward for " + port);
            commandExecutor.exec(String.format("VBoxManage controlvm " + vmName + " natpf1 %d,tcp,127.0.0.1,%d,,%d", port, port, port));
        }
    }

    private boolean isPortForwarded(String vmName, int port) {
        return getPortForwards(vmName).contains(port);
    }

    List<Integer> getPortForwards(String vmName) {
        String output = commandExecutor.exec("VBoxManage showvminfo " + vmName + " --details");
        Pattern nicRulePattern = Pattern.compile("NIC\\s.\\sRule.*host\\sport\\s=\\s([0-9]*).*");
        List<Integer> ports = new ArrayList<>();
        for (String line : output.split(System.getProperty("line.separator"))) {
            Matcher matcher = nicRulePattern.matcher(line);
            if (matcher.find()) {
                ports.add(Integer.parseInt(matcher.group(1)));
            }
        }
        return ports;
    }

    void recreatePortForward(String vmName, int port) {
        deletePortForward(vmName, port);
        createPortForward(vmName, port);
    }

    List<String> getVmNames() {
        List<String> vmNames = new ArrayList<>();
        for (String line : commandExecutor.exec("VBoxManage list runningvms").split(System.getProperty("line.separator"))) {
            vmNames.add(line.replaceFirst("\"(.*)\".*", "$1"));
        }

        return vmNames;
    }
}
