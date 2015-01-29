package com.alexecollins.docker.orchestration.model;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.regex.Pattern;

public class Ping {

	@JsonProperty(required = true)
	private URI url;
	@JsonProperty
	private int timeout = 30 * 1000;
	@JsonProperty
	private Pattern pattern;

	public URI getUrl() {
		return url;
	}

	public int getTimeout() {
		return timeout;
	}

	public Pattern getPattern() {
		return pattern;
	}
}
