package com.alexecollins.docker.orchestration;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DockerfileValidatorTest {

    private final DockerfileValidator validator = new DockerfileValidator();

    @Test
    public void goodFilePassesValidation() throws Exception {
        validator.validate(new File("src/test/docker/app"));
    }

    @Test
    public void filterAddFilePassesValidation() throws Exception {
        validator.validate(new File("src/test/docker/filter"));
    }

    @Test
    public void badFileFailValidation() throws Exception {

        try {
            validator.validate(new File("src/test/wrongDocker"));
            fail("Validate doesn't detect a wrong formatted Dockerfile");
        } catch (Exception e) {
            assertEquals("Missing or misplaced FROM on line [1] of src/test/wrongDocker/Dockerfile, found WRONG wrong command", e.getMessage());
        }

    }
}