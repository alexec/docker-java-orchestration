package com.alexecollins.docker.orchestration.util;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LogsTest {

    @Test
    public void shouldCompletelyTrimDockerHeaderBytes() throws Exception {
        String[] fakeLogs = new String[1];
        byte[] byteArray = new byte[8];
        byteArray[0] = new Byte("0001");
        byteArray[1] = new Byte("0000");
        byteArray[2] = new Byte("0000");
        byteArray[3] = new Byte("0000");
        byteArray[4] = new Byte("0000");
        byteArray[5] = new Byte("0000");
        byteArray[6] = new Byte("0000");
        byteArray[7] = new Byte("0000");

        fakeLogs[0] = new String(byteArray);

        String result = Logs.trimDockerLogHeaders(IOUtils.toInputStream(fakeLogs[0], "UTF-8"));

        assertEquals("\tSTDOUT: ", result);
    }

    @Test
    public void shouldTrimDockerHeaderBytesAndLeaveCharacter() throws Exception {
        String[] fakeLogs = new String[1];
        byte[] byteArray = new byte[9];
        byteArray[0] = new Byte("0002");
        byteArray[1] = new Byte("0000");
        byteArray[2] = new Byte("0000");
        byteArray[3] = new Byte("0000");
        byteArray[4] = new Byte("0000");
        byteArray[5] = new Byte("0000");
        byteArray[6] = new Byte("0000");
        byteArray[7] = new Byte("0000");
        byteArray[8] = new Byte("0065");

        fakeLogs[0] = new String(byteArray);

        String result = Logs.trimDockerLogHeaders(IOUtils.toInputStream(fakeLogs[0], "UTF-8"));

        assertEquals("\tSTDERR: A", result);
    }

    @Test
    public void shouldIgnoreNonDockerHeaderBytes() throws Exception {
        String[] fakeLogs = new String[1];
        byte[] byteArray = new byte[9];
        byteArray[0] = new Byte("0065");
        byteArray[1] = new Byte("0065");
        byteArray[2] = new Byte("0065");
        byteArray[3] = new Byte("0065");
        byteArray[4] = new Byte("0065");
        byteArray[5] = new Byte("0065");
        byteArray[6] = new Byte("0065");
        byteArray[7] = new Byte("0065");
        byteArray[8] = new Byte("0065");

        fakeLogs[0] = new String(byteArray);

        String result = Logs.trimDockerLogHeaders(IOUtils.toInputStream(fakeLogs[0], "UTF-8"));

        assertEquals("AAAAAAAAA", result);
    }

    @Test
    public void shouldNotChangeHeaderIfLessThan8Byes() throws Exception {
        String[] fakeLogs = new String[1];
        byte[] byteArray = new byte[7];
        byteArray[0] = new Byte("0001");
        byteArray[1] = new Byte("0000");
        byteArray[2] = new Byte("0000");
        byteArray[3] = new Byte("0000");
        byteArray[4] = new Byte("0000");
        byteArray[5] = new Byte("0000");
        byteArray[6] = new Byte("0000");

        fakeLogs[0] = new String(byteArray);

        String result = Logs.trimDockerLogHeaders(IOUtils.toInputStream(fakeLogs[0], "UTF-8"));

        assertEquals("\u0001\u0000\u0000\u0000\u0000\u0000\u0000", result);
    }
}