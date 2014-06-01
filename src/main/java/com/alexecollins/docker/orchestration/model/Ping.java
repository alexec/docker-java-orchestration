package com.alexecollins.docker.orchestration.model;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;

public class Ping {

	@JsonProperty
	private URI url;
	@JsonProperty(required = false)
	private int timeout = 30 * 1000;

	public URI getUrl() {
		return url;
	}

	public int getTimeout() {
		return timeout;
	}
}
