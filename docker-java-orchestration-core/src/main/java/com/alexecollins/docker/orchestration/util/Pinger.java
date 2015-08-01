package com.alexecollins.docker.orchestration.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Scanner;
import java.util.regex.Pattern;

import static java.lang.System.currentTimeMillis;

public final class Pinger {

    private static final Logger logger = LoggerFactory.getLogger(Pinger.class);

    private Pinger() {
    }

    public static boolean ping(URI uri, Pattern pattern, int timeout, boolean sslVerify) {
        long start = currentTimeMillis();
        while (!ping(uri, pattern, sslVerify)) {
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
        return ping(uri, null, timeout, true);
    }

    private static boolean ping(URI uri, Pattern pattern, boolean sslVerify) {
        try {
            final HttpURLConnection c = (HttpURLConnection) uri.toURL().openConnection();
            c.setRequestProperty("Accept", "*/*");

            if (c instanceof HttpsURLConnection && !sslVerify) {
                ((HttpsURLConnection) c).setSSLSocketFactory(getInsecureSSLSocketFactory());
                ((HttpsURLConnection) c).setHostnameVerifier(getInsecureHostnameVerifier());
            }

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

    private static final SSLSocketFactory getInsecureSSLSocketFactory() {
        SSLSocketFactory sslSocketFactory = null;
        try {
            TrustManager[] tm = new TrustManager[]{new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
            }};
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tm, null);

            return context.getSocketFactory();
        } catch (KeyManagementException e) {
            logger.error("Failed to initialise SSLContext", e);
        } catch (NoSuchAlgorithmException e) {
            logger.error("No TLS algorithm support", e);
        }
        return sslSocketFactory;
    }

    private static HostnameVerifier getInsecureHostnameVerifier() {
        return new HostnameVerifier() {
            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        };
    }
}
