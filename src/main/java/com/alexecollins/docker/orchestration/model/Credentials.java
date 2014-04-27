package com.alexecollins.docker.orchestration.model;

public class Credentials {
	public final String username;
	public final String password;
	public final String email;

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
		this.username = username;
		this.password = password;
		this.email = email;
	}
}
