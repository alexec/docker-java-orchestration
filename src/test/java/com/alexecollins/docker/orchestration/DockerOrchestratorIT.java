package com.alexecollins.docker.orchestration;

import com.alexecollins.docker.orchestration.model.Id;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;


public class DockerOrchestratorIT {
    File src = new File("src/test/docker");
    File workDir = new File("target/docker");
    File projDir = new File("");
    DockerOrchestrator orchestrator;
    DockerClient docker;

    @After
    public void tearDown() throws Exception {
        if (orchestrator != null)
            orchestrator.clean();
    }

    @Before
    public void setUp() throws Exception {
        DockerClientConfig.DockerClientConfigBuilder confgBuilder = DockerClientConfig.createDefaultConfigBuilder()
                .withUri(DockerOrchestrator.DEFAULT_HOST)
                .withUsername("alexec")
                .withPassword(System.getProperty("docker.password", ""))
                .withEmail("alex.e.c@gmail.com")
                .withVersion("1.9");

        docker = DockerClientBuilder.getInstance(confgBuilder.build()).build();

        assert docker.authConfig() != null && docker.authConfig().getUsername() != null;

        orchestrator = new DockerOrchestrator(
                docker,
                src, workDir, projDir, "docker-java-orchestrator"
                ,
                DockerOrchestrator.DEFAULT_FILTER, DockerOrchestrator.DEFAULT_PROPERTIES);
    }

    @Test
    public void testList() throws Exception {
        assertEquals(3, orchestrator.ids().size());
    }

    @Test
    public void whenWeCleanThenAllImagesAreDeleted() throws Exception {

        final List<Image> expectedImages = docker.listImagesCmd().exec();

        orchestrator.build(new Id("busybox"));
        orchestrator.clean(new Id("busybox"));

        assertEquals(expectedImages.size(), docker.listImagesCmd().exec().size());
    }

    @Test
    public void whenWeCleanThenAllContainersAreDeleted() throws Exception {

        final List<Container> expectedContainers = docker.listContainersCmd().withShowAll(true).exec();

        orchestrator.build(new Id("busybox"));
        orchestrator.clean(new Id("busybox"));

        assertEquals(expectedContainers.size(), docker.listContainersCmd().withShowAll(true).exec().size());
    }

    @Test
    public void testBuild() throws Exception {
        orchestrator.build();
    }

    @Test
    public void testStart() throws Exception {
        orchestrator.start();
    }

    @Test
    public void testStop() throws Exception {
        orchestrator.stop();
    }

    @Test
    public void testPush() throws Exception {
        orchestrator.push();
    }

    @Test
    public void testIsRunning() throws Exception {
        orchestrator.isRunning();
    }
}
