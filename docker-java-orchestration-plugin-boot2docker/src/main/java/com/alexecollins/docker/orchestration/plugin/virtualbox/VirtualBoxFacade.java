package com.alexecollins.docker.orchestration.plugin.virtualbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class VirtualBoxFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualBoxFacade.class);

    private static String exec(String command) {
        LOGGER.debug("Executing " + command);
        int exitCode;
        StreamRecorder error;
        StreamRecorder output;
        try {
            Process process = Runtime.getRuntime().exec(command);

            error = new StreamRecorder(process.getErrorStream(), "ERROR");
            output = new StreamRecorder(process.getInputStream(), "OUTPUT");

            error.start();
            output.start();
            exitCode = process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String outputString = output.toString();
        LOGGER.debug(outputString.toString());
        if (exitCode != 0) {
            throw new RuntimeException("exit code " + exitCode + ", " + error.toString());
        } else {
            return outputString;
        }
    }

    private static String asString(InputStream inputStream) throws IOException {
        BufferedReader stderr = new BufferedReader(new InputStreamReader(inputStream));
        String l;
        StringBuilder errorMessage = new StringBuilder();
        while ((l = stderr.readLine()) != null) {
            errorMessage.append(l).append('\n');
        }
        return errorMessage.toString().trim();
    }

    void deletePortForward(int port) {
        if (isPortForwarded(port)) {
            LOGGER.info("Deleting VirtualBox port forward for " + port);
            exec(String.format("VBoxManage controlvm boot2docker-vm natpf1 delete %d", port));
        }
    }

    void createPortForward(int port) {
        if (!isPortForwarded(port)) {
            LOGGER.info("Creating VirtualBox port forward for " + port);
            exec(String.format("VBoxManage controlvm boot2docker-vm natpf1 %d,tcp,127.0.0.1,%d,,%d", port, port, port));
        }
    }

    private boolean isPortForwarded(int port) {
        return getPortForwards().contains(port);
    }

    List<Integer> getPortForwards() {
        String output = exec("VBoxManage showvminfo boot2docker-vm --details");
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

    void recreatePortForward(int port) {
        deletePortForward(port);
        createPortForward(port);
    }
}
