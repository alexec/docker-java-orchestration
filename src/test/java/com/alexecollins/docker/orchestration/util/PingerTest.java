package com.alexecollins.docker.orchestration.util;

import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PingerTest {

	@Test
	public void testGoogle() throws Exception {
		assertTrue(Pinger.ping(URI.create("http://www.google.com")));
	}

	@Test
	public void testNoop() throws Exception {
		assertFalse(Pinger.ping(URI.create("http://noop")));
	}
}