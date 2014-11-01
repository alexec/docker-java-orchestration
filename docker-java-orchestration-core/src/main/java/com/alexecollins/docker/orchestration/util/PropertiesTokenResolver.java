package com.alexecollins.docker.orchestration.util;

import java.util.Properties;

public class PropertiesTokenResolver implements TokenResolver {

    private final Properties properties;

    public PropertiesTokenResolver(Properties properties) {
        super();
        this.properties = properties;
    }

    @Override
    public String resolveToken(String tokenName) {
        final Object value = properties.get(tokenName);
        return (value == null) ? null : value.toString();
    }
}
