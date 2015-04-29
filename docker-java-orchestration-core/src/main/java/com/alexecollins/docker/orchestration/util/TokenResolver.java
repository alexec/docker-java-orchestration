package com.alexecollins.docker.orchestration.util;

interface TokenResolver {
    String resolveToken(String tokenName);
}
