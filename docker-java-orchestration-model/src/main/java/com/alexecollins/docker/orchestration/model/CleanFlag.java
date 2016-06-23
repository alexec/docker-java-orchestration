package com.alexecollins.docker.orchestration.model;

/**
 *
 * Flag to signal what kind of cleaning could be done
 */
public enum CleanFlag {
    CONTAINER,
    IMAGE,
    FORCE;
}
