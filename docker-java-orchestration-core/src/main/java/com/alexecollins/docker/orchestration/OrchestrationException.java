package com.alexecollins.docker.orchestration;

@SuppressWarnings("WeakerAccess")
class OrchestrationException extends RuntimeException {
    OrchestrationException(String message) {
        super(message);
    }

    OrchestrationException(String message, Throwable cause) {
        super(message, cause);
    }

    OrchestrationException(Throwable cause) {
        super(cause);
    }
}
