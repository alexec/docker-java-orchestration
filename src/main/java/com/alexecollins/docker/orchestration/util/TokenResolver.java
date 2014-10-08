package com.alexecollins.docker.orchestration.util;

public interface TokenResolver {
    String resolveToken(String tokenName);
}
