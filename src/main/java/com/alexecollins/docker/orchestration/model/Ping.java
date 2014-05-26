package com.alexecollins.docker.orchestration.model;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;

public class Ping {

	@JsonProperty
	public URI url;
	@JsonProperty(required = false)
	public int timeout = 30 * 1000;
}
