package com.alexecollins.docker.orchestration;

import com.alexecollins.docker.orchestration.model.BuildFlag;
import com.alexecollins.docker.orchestration.model.CleanFlag;
import com.alexecollins.docker.orchestration.model.Id;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class DockerOrchestratorIT {
    private final File src = new File("src/test/docker");
    private final File workDir = new File("target/docker");
    private final File projDir = new File("");
    private DockerOrchestrator orchestrator;
    private DockerClient docker;

    private static Properties properties() throws IOException {
        Properties properties = new Properties();
        File file = File.createTempFile("testFile", "txt");
        properties.setProperty("testFile.path", file.getCanonicalPath());
        properties.setProperty("testFile.name", file.getName());
        return properties;
    }

    private static boolean runningOnCircleCi() {
        return System.getenv().containsKey("CIRCLE_PROJECT_REPONAME");
    }

    @After
    public void tearDown() throws Exception {
        if (orchestrator != null) {
            try {
                orchestrator.clean();
            } catch (Exception e) {
                System.err.println("ignoring exception in tear down " + e);
            }
        }
    }

    @Before
    public void setUp() throws Exception {

        System.out.println("runningOnCircleCi=" + runningOnCircleCi());

        docker = DockerClientBuilder.getInstance(DockerClientConfig.createDefaultConfigBuilder().build()).build();

        orchestrator = DockerOrchestrator.builder()
                .docker(docker)
                .src(src)
                .workDir(workDir)
                .rootDir(projDir)
                .user("registry")
                .properties(properties())
                .project("docker-java-orchestrator")
                .buildFlags(EnumSet.of(BuildFlag.REMOVE_INTERMEDIATE_IMAGES))
                .permissionErrorTolerant(true)
                .build();
    }

    @Test
    public void listsAllDefinintions() throws Exception {
        assertEquals(Arrays.asList(new Id("busybox"), new Id("disabled"), new Id("mysql"), new Id("app")), orchestrator.ids());
    }

    @Test
    public void whenWeCleanThenAllImagesAreDeleted() throws Exception {

        final List<Image> expectedImages = docker.listImagesCmd().exec();

        orchestrator.build(new Id("busybox"));
        orchestrator.clean(new Id("busybox"));

        int expectedSize = expectedImages.size() + (runningOnCircleCi() ? 1 : 0);
        assertEquals(expectedSize, docker.listImagesCmd().exec().size());
    }

    @Test
    public void whenWeCleanThenAllContainersAreDeleted() throws Exception {

        // I *think* that cleaning a container can sometimes remove two images
        orchestrator.clean(new Id("busybox"));
        final List<Container> expectedContainers = docker.listContainersCmd().withShowAll(true).exec();

        orchestrator.build(new Id("busybox"));
        try {
            orchestrator.clean(new Id("busybox"));
        } catch (OrchestrationException e) {
            assertTrue(runningOnCircleCi());
            return;
        }

        int expectedSize = expectedContainers.size() + (runningOnCircleCi() ? 1 : 0);
        assertEquals(expectedSize, docker.listContainersCmd().withShowAll(true).exec().size());
    }

    @Test
    public void whenWeCleanThenOnlyContainersAreDeleted() throws Exception {
        final List<Container> expectedContainers = docker.listContainersCmd().withShowAll(true).exec();

        final List<Image> expectedImages = docker.listImagesCmd().exec();

        orchestrator.build(new Id("busybox"));
        orchestrator.clean(new Id("busybox"), CleanFlag.CONTAINER_ONLY);

        int expectedContainersSize = expectedContainers.size() + (runningOnCircleCi() ? 1 : 0);
        assertEquals(expectedContainersSize, docker.listContainersCmd().withShowAll(true).exec().size());

        int expectedImageSize = expectedImages.size() + (runningOnCircleCi() ? 1 : 0) + 1;
        assertEquals(expectedImageSize, docker.listImagesCmd().exec().size());

        orchestrator.clean(new Id("busybox"));
    }

    @Test
    public void testBuild() throws Exception {
        orchestrator.build();
    }

    @Test
    public void startingSmokesAndDoesNotStartDisabledContainer() throws Exception {
        orchestrator.start();

        assertThat(orchestrator.getPlugin(TestPlugin.class).getStarted(), not(hasItem(new Id("disabled"))));
    }

    @Test
    public void testStop() throws Exception {
        orchestrator.stop();
    }

    @Ignore("no private repo to test with")
    @Test
    public void testPush() throws Exception {
        orchestrator.build();
        orchestrator.push();
    }

    @Test
    public void testIsRunning() throws Exception {
        orchestrator.isRunning();
    }


}
