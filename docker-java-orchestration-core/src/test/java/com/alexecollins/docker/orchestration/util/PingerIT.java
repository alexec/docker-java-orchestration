package com.alexecollins.docker.orchestration.util;

import org.junit.Test;

import java.net.URI;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PingerIT {

    @Test
    public void testGoogle() throws Exception {
        assertTrue(Pinger.ping(URI.create("http://www.google.com"), 5000));
    }

    @Test
    public void ensureRegexpMatches() throws Exception {
        assertTrue(Pinger.ping(URI.create("http://www.alexecollins.com"), Pattern.compile("Alex Collins"), 1000));
    }

    @Test
    public void ensureBadRegexpDoesNotMatch() throws Exception {
        assertFalse(Pinger.ping(URI.create("http://www.alexecollins.com"), Pattern.compile("Bill Murray"), 1000));
    }

    @Test
    public void testNoop() throws Exception {
        assertFalse(Pinger.ping(URI.create("http://noop"), 5000));
    }
}