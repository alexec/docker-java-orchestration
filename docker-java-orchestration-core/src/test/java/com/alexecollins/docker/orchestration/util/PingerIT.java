package com.alexecollins.docker.orchestration.util;

import com.alexecollins.docker.orchestration.TestLogger;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PingerIT {

    private final int timeout = 100;
    @Rule
    public TestRule testName = new TestLogger();
    private HttpServer httpServer;
    private URI httpServerAddress;

    @Before
    public void setUp() throws Exception {
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange httpExchange) throws IOException {
                byte[] body = "Foo".getBytes();
                httpExchange.sendResponseHeaders(200, body.length);
                httpExchange.getResponseBody().write(body);
                httpExchange.getResponseBody().flush();
                httpExchange.close();
            }
        });
        httpServer.start();
        httpServerAddress = URI.create(String.format("http://localhost:%d/", httpServer.getAddress().getPort()));

    }

    @After
    public void tearDown() throws Exception {
        httpServer.stop(0);
    }

    @Test
    public void validHost() throws Exception {
        assertTrue(Pinger.ping(httpServerAddress, timeout));
    }

    @Test
    public void ensureRegexpMatches() throws Exception {
        assertTrue(Pinger.ping(httpServerAddress, Pattern.compile("Foo"), timeout, true));
    }

    @Test
    public void ensureBadRegexpDoesNotMatch() throws Exception {
        assertFalse(Pinger.ping(httpServerAddress, Pattern.compile("Bill Murray"), timeout, true));
    }

    @Test
    public void invalidHost() throws Exception {
        assertFalse(Pinger.ping(URI.create("http://noop"), timeout));
    }
}