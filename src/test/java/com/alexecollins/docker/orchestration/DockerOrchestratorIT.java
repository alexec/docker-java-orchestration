package com.alexecollins.docker.orchestration;

import com.alexecollins.docker.orchestration.model.Credentials;
import com.alexecollins.docker.orchestration.model.Id;
import com.kpelykh.docker.client.DockerClient;
import com.kpelykh.docker.client.model.Container;
import com.kpelykh.docker.client.model.Image;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class DockerOrchestratorIT {
	File src = new File("src/test/docker");
	File workDir = new File("target/docker");
    File projDir = new File("");
	DockerOrchestrator orchestrator;
	DockerClient docker;

	@After
	public void tearDown() throws Exception {
		orchestrator.clean();
	}

	@Before
	public void setUp() throws Exception {
		docker = new DockerClient(DockerOrchestrator.DEFAULT_HOST, "1.9");
		orchestrator = new DockerOrchestrator(
				docker,
				src, workDir, projDir, "docker-java-orchestrator",
				new Credentials("alexec", System.getProperty("docker.password", ""), "alex.e.c@gmail.com"),
				DockerOrchestrator.DEFAULT_FILTER, DockerOrchestrator.DEFAULT_PROPERTIES);
	}

	@Test
	public void testList() throws Exception {
		assertEquals(3, orchestrator.ids().size());
	}

	@Test
	public void whenWeCleanThenAllImagesAreDeleted() throws Exception {

		final List<Image> expectedImages = docker.getImages();

		orchestrator.build(new Id("busybox"));
		orchestrator.clean(new Id("busybox"));

		assertEquals(expectedImages, docker.getImages());
	}

    @Ignore("quarantine")
    @Test
    public void whenWeCleanThenAllContainersAreDeleted() throws Exception {

        final List<Container> expectedContainers = docker.listContainers(true);

        orchestrator.build(new Id("busybox"));
        orchestrator.clean(new Id("busybox"));

        assertEquals(expectedContainers, docker.listContainers(true));
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
