package com.alexecollins.docker.orchestration;

@SuppressWarnings("WeakerAccess")
class OrchestrationException extends RuntimeException {
    OrchestrationException(String message) {
        super(message);
    }

    OrchestrationException(Throwable cause) {
        super(cause);
    }
}
