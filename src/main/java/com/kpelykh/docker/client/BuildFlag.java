package com.kpelykh.docker.client;

/**
 * Build flags for DockerOrchestrator. These are here for backward compatibility.
 * Newer docker-java does not contain these.
 */
public enum BuildFlag {
    NO_CACHE,
    REMOVE_INTERMEDIATE_IMAGES
}
