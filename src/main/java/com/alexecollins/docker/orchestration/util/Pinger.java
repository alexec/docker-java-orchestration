package com.alexecollins.docker.orchestration.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;

import static java.lang.System.currentTimeMillis;

public final class Pinger {
	private Pinger() {
	}

	public static boolean ping(URI uri, int timeout) {
		long start = currentTimeMillis();
		while (!ping(uri)) {
			if (currentTimeMillis() - start > timeout) {
				return false;
			}
		}
		return true;
	}

	public static boolean ping(URI uri) {
		try {
			final HttpURLConnection c = (HttpURLConnection) uri.toURL().openConnection();
			c.setRequestProperty("Accept", "*/*");
			c.connect();
			try {
				return c.getResponseCode() == 200;
			} finally {
				c.disconnect();
			}
		} catch (IOException e){
			return false;
		}
	}
}
