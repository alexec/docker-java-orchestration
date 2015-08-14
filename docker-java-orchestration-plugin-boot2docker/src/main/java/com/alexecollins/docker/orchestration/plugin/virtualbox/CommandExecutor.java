package com.alexecollins.docker.orchestration.plugin.virtualbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CommandExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandExecutor.class);

    String exec(String command) {
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
        LOGGER.debug(outputString);
        if (exitCode != 0) {
            throw new RuntimeException("exit code " + exitCode + ", " + error.toString());
        } else {
            return outputString;
        }
    }
}
