package com.alexecollins.docker.orchestration.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class Logs {
    /**
     * Parse Docker container logs.
     * <p/>
     * See: http://docs.docker.com/v1.6/reference/api/docker_remote_api_v1.13/#attach-to-a-container
     *
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

            if (BytePrefix.isPrefix(stringAsBytes[0])) {
                String prefix = String.format("\t%s: ", BytePrefix.findFor(stringAsBytes[0]).getPrefix());
                dockerContainerLog[i] = prefix + new String(Arrays.copyOfRange(stringAsBytes, 8, stringAsBytes.length));
            }
        }

        return StringUtils.join(dockerContainerLog, System.lineSeparator());
    }

    public enum BytePrefix {
        StdOut(1, "STDOUT"),
        StdErr(2, "STDERR");

        private final Byte headerByte;
        private final String prefix;

        BytePrefix(int headerByte, String prefix) {
            this.headerByte = (byte) headerByte;
            this.prefix = prefix;
        }

        public static BytePrefix findFor(Byte bytes) {
            for (BytePrefix prefix : values())
                if (bytes.equals(prefix.headerByte))
                    return prefix;

            return BytePrefix.StdOut;
        }

        public static boolean isPrefix(Byte bytes) {
            for (BytePrefix prefix : values())
                if (bytes.equals(prefix.headerByte))
                    return true;

            return false;
        }

        public Byte getHeaderByte() {
            return headerByte;
        }

        public String getPrefix() {
            return prefix;
        }
    }
}
