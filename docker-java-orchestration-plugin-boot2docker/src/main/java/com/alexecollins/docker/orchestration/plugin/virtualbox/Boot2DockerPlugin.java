package com.alexecollins.docker.orchestration.plugin.virtualbox;

import com.alexecollins.docker.orchestration.model.Conf;
import com.alexecollins.docker.orchestration.model.Id;
import com.alexecollins.docker.orchestration.plugin.api.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Boot2DockerPlugin implements Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(Boot2DockerPlugin.class);

    @Override
    public void started(Id id, Conf conf) {
        for (String stringPort : conf.getPorts()) {
            int port = Integer.parseInt(stringPort.split(" ")[0]);
            LOGGER.info("Setting up VirtualBox port forward for " + port);
            try {
                deletePortForward(port);
            } catch (RuntimeException e) {
                if (!e.getMessage().contains("NS_ERROR_INVALID_ARG")) {
                    throw e;
                }
            }
            createPortForward(port);
        }
    }

    private void deletePortForward(int port) {
        exec("VBoxManage controlvm boot2docker-vm natpf1 delete " + port);
    }

    private void createPortForward(int port) {
        exec("VBoxManage controlvm boot2docker-vm natpf1 " + port + ",tcp,127.0.0.1," + port + ",," + port + "");
    }

    private void exec(String command) {
        LOGGER.info("Executing " + command);
        int exitCode;
        String message;
        try {
            Process process = Runtime.getRuntime().exec(command);
            message = extractStdErr(process);
            logStdOut(process);
            exitCode = process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (exitCode != 0) {
            throw new RuntimeException("exit code " + exitCode + ", "  + message);
        }
    }

    private void logStdOut(Process process) throws IOException {
        BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String l;
        while ((l = stdout.readLine()) != null) {
            LOGGER.info(l);
        }
    }

    private String extractStdErr(Process process) throws IOException {
        BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        String l;
        StringBuilder errorMessage = new StringBuilder();
        while ((l = stderr.readLine()) != null) {
            errorMessage.append(l).append(' ');
        }
        return errorMessage.toString().trim();
    }
}
