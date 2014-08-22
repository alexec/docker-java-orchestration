package com.alexecollins.docker.orchestration.model;

import com.github.dockerjava.api.model.AuthConfig;

public class Credentials extends AuthConfig{

	public Credentials(String username, String password, String email) {
		if (username == null) {
			throw new IllegalArgumentException("username is null");
		}
		if (password == null) {
			throw new IllegalArgumentException("password is null");
		}
		if (email == null) {
			throw new IllegalArgumentException("email is null");
		}
        setUsername(username);
        setPassword(password);
        setEmail(email);
	}
}
