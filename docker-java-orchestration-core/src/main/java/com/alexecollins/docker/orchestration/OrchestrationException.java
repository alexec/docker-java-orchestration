package com.alexecollins.docker.orchestration;

public class OrchestrationException extends RuntimeException {
    OrchestrationException(String message) {
        super(message);
    }

    OrchestrationException(Throwable cause) {
        super(cause);
    }
}
