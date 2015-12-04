package com.alexecollins.docker.orchestration.model;

/**
 * Build flags for DockerOrchestrator.
 */
public enum BuildFlag {
    NO_CACHE,
    REMOVE_INTERMEDIATE_IMAGES,
    QUIET,
    PULL
}
