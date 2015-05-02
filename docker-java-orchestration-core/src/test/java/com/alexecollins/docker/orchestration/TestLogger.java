package com.alexecollins.docker.orchestration;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestLogger extends TestWatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestLogger.class);

    @Override
    protected void starting(Description description) {
        LOGGER.info("--- starting {} ---", description);
    }

    @Override
    protected void finished(Description description) {
        LOGGER.info("--- finished {} ---", description);
    }
}
