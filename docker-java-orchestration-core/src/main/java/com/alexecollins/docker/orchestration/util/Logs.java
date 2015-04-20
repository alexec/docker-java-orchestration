package com.alexecollins.docker.orchestration.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class Logs {

    public static final Byte STDOUT_BYTE = new Byte("0001");
    public static final Byte STDERR_BYTE = new Byte("0002");

    /**
     * Parse Docker container logs.
     *
     * See: http://docs.docker.com/v1.6/reference/api/docker_remote_api_v1.13/#attach-to-a-container
     * @param stream
     * @return
     * @throws IOException
     */
    public static String trimDockerLogHeaders(InputStream stream) throws IOException {
        String[] dockerContainerLog = IOUtils.toString(stream).split(System.lineSeparator());

        for (int i = 0; i < dockerContainerLog.length; i++) {
            byte[] stringAsBytes = dockerContainerLog[i].getBytes();

            if (stringAsBytes.length < 8) {
                continue;
            }

            if (STDERR_BYTE.equals(stringAsBytes[0]) || STDOUT_BYTE.equals(stringAsBytes[0])) {
                String prefix = "\t" + (STDERR_BYTE.equals(stringAsBytes[0]) ? "STDERR: " : "STDOUT: ");
                dockerContainerLog[i] = prefix + new String(Arrays.copyOfRange(stringAsBytes, 8, stringAsBytes.length));
            }
        }

        return StringUtils.join(dockerContainerLog, System.lineSeparator());
    }
}
