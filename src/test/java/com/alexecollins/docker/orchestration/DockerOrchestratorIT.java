package com.alexecollins.docker.orchestration;

import com.alexecollins.docker.orchestration.model.Credentials;
import com.kpelykh.docker.client.DockerClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;


public class DockerOrchestratorIT {
	File src = new File("src/test/docker");
	File workDir = new File("target/docker");
	DockerOrchestrator orchestrator;

	@After
	public void tearDown() throws Exception {
		orchestrator.clean();
	}

	@Before
	public void setUp() throws Exception {
		orchestrator = new DockerOrchestrator(
				new DockerClient(DockerOrchestrator.DEFAULT_HOST, "1.9"),
				src, workDir, "docker-java-orchestrator",
				new Credentials("alexec", System.getProperty("docker.password"), "alex.e.c@gmail.com"),
				DockerOrchestrator.DEFAULT_FILTER, DockerOrchestrator.DEFAULT_PROPERTIES);
	}

	@Test
	public void testList() throws Exception {
		assertEquals(3, orchestrator.ids().size());
	}

	@Test
	public void testClean() throws Exception {
		orchestrator.clean();
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
}
