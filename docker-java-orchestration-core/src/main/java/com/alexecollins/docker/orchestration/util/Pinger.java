package com.alexecollins.docker.orchestration.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Scanner;
import java.util.regex.Pattern;

import static java.lang.System.currentTimeMillis;

public final class Pinger {
    private Pinger() {
    }

    public static boolean ping(URI uri, Pattern pattern, int timeout) {
        long start = currentTimeMillis();
        while (!ping(uri, pattern)) {
            if (currentTimeMillis() - start > timeout) {
                return false;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    public static boolean ping(URI uri, int timeout) {
        return ping(uri, null, timeout);
    }

    private static boolean ping(URI uri, Pattern pattern) {
        try {
            final HttpURLConnection c = (HttpURLConnection) uri.toURL().openConnection();
            c.setRequestProperty("Accept", "*/*");
            c.connect();
            try {
                return c.getResponseCode() == 200 &&
                        (pattern == null || pattern.matcher(readAll(c.getInputStream())).find());
            } finally {
                c.disconnect();
            }
        } catch (IOException e) {
            return false;
        }
    }

    private static String readAll(InputStream inputStream) {
        return new Scanner(inputStream).useDelimiter("\\A").next();
    }

}
