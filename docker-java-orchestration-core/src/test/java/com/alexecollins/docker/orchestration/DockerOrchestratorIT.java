package com.alexecollins.docker.orchestration;

import com.alexecollins.docker.orchestration.model.BuildFlag;
import com.alexecollins.docker.orchestration.model.CleanFlag;
import com.alexecollins.docker.orchestration.model.Conf;
import com.alexecollins.docker.orchestration.model.Id;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
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

        docker = DockerClientBuilder.getInstance(DefaultDockerClientConfig.createDefaultConfigBuilder().build()).build();

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
                .definitionFilter(new DefinitionFilter() {
                    @Override
                    public boolean test(final Id id, @SuppressWarnings("UnusedParameters") final Conf conf) {
                        return !new Id("private-registry").equals(id);
                    }
                })
                .build();

        orchestrator.clean();
    }

    @Test
    public void listsAllDefinintions() throws Exception {
        final List<Id> expectedIds = Arrays.asList(new Id("busybox"), new Id("disabled"), new Id("mysql"),
                                                   new Id("app"), new Id("private-registry"));
        final List<Id> actualIds = orchestrator.ids();
        assertTrue("Expected " + expectedIds + " to contain all values of " + actualIds,
                expectedIds.containsAll(actualIds) && actualIds.containsAll(expectedIds));
    }

    @Test
    public void whenWeCleanThenAllImagesAreDeleted() throws Exception {

        int numImagesBefore = getNumImages();

        orchestrator.build(new Id("busybox"));
        orchestrator.clean(new Id("busybox"));

        int expectedNumImages = numImagesBefore + (runningOnCircleCi() ? 1 : 0);
        int numImagesAfter = getNumImages();

        assertEquals(expectedNumImages, numImagesAfter);
    }

    @Test
    public void whenWeCleanThenAllContainersAreDeleted() throws Exception {

        // I *think* that cleaning a container can sometimes remove two images
        orchestrator.clean(new Id("busybox"));
        int numContainersBefore = getNumContainers();

        orchestrator.build(new Id("busybox"));
        try {
            orchestrator.clean(new Id("busybox"));
        } catch (OrchestrationException e) {
            assertTrue(runningOnCircleCi());
            return;
        }

        assertEquals(numContainersBefore, getNumContainers());
    }

    @Test
    public void whenWeCleanThenOnlyContainersAreDeleted() throws Exception {

        int numContainersBefore = getNumContainers();
        int numImagesBefore = getNumImages();

        orchestrator.build(new Id("busybox"));
        orchestrator.clean(new Id("busybox"), EnumSet.of(CleanFlag.CONTAINER));

        assertEquals(numContainersBefore, getNumContainers());

        int expectedNumImages = numImagesBefore + 1;
        assertEquals(expectedNumImages, getNumImages());

        orchestrator.clean(new Id("busybox"));
    }

    private int getNumImages() {
        return docker.listImagesCmd().exec().size();
    }

    private int getNumContainers() {
        return docker.listContainersCmd().withShowAll(true).exec().size();
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

    @Test
    public void saveCreatesAppTarFile() throws Exception {
        orchestrator.build();

        File destDir = new File("target");
        orchestrator.save(destDir, false);

        assertTrue(new File(destDir, "app.tar").exists());
    }
}
