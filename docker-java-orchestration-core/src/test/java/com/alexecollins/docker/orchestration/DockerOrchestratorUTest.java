package com.alexecollins.docker.orchestration;


import com.alexecollins.docker.orchestration.model.Conf;
import com.alexecollins.docker.orchestration.model.HealthChecks;
import com.alexecollins.docker.orchestration.model.Id;
import com.alexecollins.docker.orchestration.model.Link;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerConfig;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.client.ClientResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DockerOrchestratorUTest {

    private static final String IMAGE_NAME = "theImage";
    private static final String IMAGE_ID = "imageId";

    private static final String CONTAINER_NAME = "theContainer";
    private static final String CONTAINER_ID = "containerId";

    private static final String TAG_NAME = "test-tag";

    private final Logger logger = mock(Logger.class);

    @Mock private DockerClient dockerMock;
    @Mock private Repo repoMock;
    @Mock private File fileMock;
    @Mock private File srcFileMock;
    @Mock private Id idMock;
    @Mock private FileOrchestrator fileOrchestratorMock;
    @Mock private ClientResponse clientResponseMock;
    @Mock private Conf confMock;
    @Mock private CreateContainerResponse createContainerResponse;
    @Mock private ContainerConfig containerConfigMock;
    @Mock private Container containerMock;
    @Mock private InspectContainerResponse containerInspectResponseMock;
    @Mock private BuildImageCmd buildImageCmdMock;
    @Mock private CreateContainerCmd createContainerCmdMock;
    @Mock private StartContainerCmd startContainerCmdMock;
    @Mock private InspectContainerCmd inspectContainerCmdMock;
    @Mock private ListContainersCmd listContainersCmdMockOnlyRunning;
    @Mock private RemoveContainerCmd removeContainerCmdMock;
    @Mock private StopContainerCmd stopContainerCmdMock;
    @Mock private TagImageCmd tagImageCmdMock;
    @Mock private PushImageCmd pushImageCmd;

    private DockerOrchestrator testObj;


    @Before
    public void setup () throws DockerException, IOException {
        testObj = new DockerOrchestrator(dockerMock, repoMock, fileOrchestratorMock, logger);

        when(repoMock.src(idMock)).thenReturn(srcFileMock);
        when(repoMock.conf(idMock)).thenReturn(confMock);
        when(repoMock.imageName(idMock)).thenReturn(IMAGE_NAME);
        when(repoMock.findImageId(idMock)).thenReturn(IMAGE_ID);
        when(repoMock.containerName(idMock)).thenReturn(CONTAINER_NAME);

        when(confMock.getLinks()).thenReturn(new ArrayList<Link>());
        when(confMock.getHealthChecks()).thenReturn(new HealthChecks());
        when(confMock.getTags()).thenReturn(Arrays.asList(IMAGE_NAME + ":" + TAG_NAME));

        when(repoMock.findImageId(idMock)).thenReturn(IMAGE_ID);
        when(repoMock.findContainer(idMock)).thenReturn(containerMock);
        when(containerMock.getId()).thenReturn(CONTAINER_ID);

        when(fileOrchestratorMock.prepare(idMock, srcFileMock, confMock)).thenReturn(fileMock);

        when(repoMock.ids(false)).thenReturn(Arrays.asList(idMock));
        when(repoMock.ids(true)).thenReturn(Arrays.asList(idMock));
        when(repoMock.tag(any(Id.class))).thenReturn(IMAGE_NAME + ":" + TAG_NAME);

        when(dockerMock.buildImageCmd(eq(fileMock))).thenReturn(buildImageCmdMock);
        when(buildImageCmdMock.withTag(any(String.class))).thenReturn(buildImageCmdMock);
        when(buildImageCmdMock.exec()).thenReturn(IOUtils.toInputStream("Successfully built"));

        when(dockerMock.createContainerCmd(IMAGE_ID)).thenReturn(createContainerCmdMock);
        when(createContainerCmdMock.exec()).thenReturn(createContainerResponse);
        when(createContainerCmdMock.withName(eq(CONTAINER_NAME))).thenReturn(createContainerCmdMock);

        when(createContainerResponse.getId()).thenReturn(CONTAINER_ID);

        when(dockerMock.startContainerCmd(CONTAINER_ID)).thenReturn(startContainerCmdMock);
        when(dockerMock.stopContainerCmd(CONTAINER_ID)).thenReturn(stopContainerCmdMock);
        when(dockerMock.removeContainerCmd(CONTAINER_ID)).thenReturn(removeContainerCmdMock);

        when(dockerMock.listContainersCmd()).thenReturn(listContainersCmdMockOnlyRunning);
        when(listContainersCmdMockOnlyRunning.withShowAll(false)).thenReturn(listContainersCmdMockOnlyRunning);
        when(listContainersCmdMockOnlyRunning.exec()).thenReturn(Collections.EMPTY_LIST);

        when(stopContainerCmdMock.withTimeout(anyInt())).thenReturn(stopContainerCmdMock);

        when(dockerMock.inspectContainerCmd(CONTAINER_ID)).thenReturn(inspectContainerCmdMock);
        when(inspectContainerCmdMock.exec()).thenReturn(containerInspectResponseMock);
        when(containerInspectResponseMock.getImageId()).thenReturn(IMAGE_ID);

        when(dockerMock.tagImageCmd(anyString(), anyString(), anyString())).thenReturn(tagImageCmdMock);
        when(tagImageCmdMock.withForce()).thenReturn(tagImageCmdMock);

        when(dockerMock.pushImageCmd(anyString())).thenReturn(pushImageCmd);
        when(pushImageCmd.withAuthConfig(any(AuthConfig.class))).thenReturn(pushImageCmd);
        when(pushImageCmd.exec()).thenReturn(IOUtils.toInputStream("{\"status\":\"The push refers to...\"}"));
    }


    @Test
    public void createAndStartNewContainer() throws DockerException, IOException {
        when(repoMock.imageExists(idMock)).thenReturn(false);
        when(repoMock.findContainer(idMock)).thenReturn(null);

        testObj.start();

        verify(createContainerCmdMock).exec();
        verify(startContainerCmdMock).exec();
    }

    @Test
    public void startExistingContainerAsImageIdsMatch() throws DockerException, IOException {
        when(repoMock.imageExists(idMock)).thenReturn(true);
        when(listContainersCmdMockOnlyRunning.exec()).thenReturn(Collections.EMPTY_LIST);

        testObj.start();

        verify(createContainerCmdMock, times(0)).exec();
        verify(startContainerCmdMock).exec();
    }

    @Test
    public void containerIsAlreadyRunning() throws DockerException, IOException {
        when(listContainersCmdMockOnlyRunning.exec()).thenReturn(Arrays.asList(containerMock));

        testObj.start();

        verify(createContainerCmdMock, times(0)).exec();
        verify(startContainerCmdMock, times(0)).exec();
    }

    @Test
    public void removeExistingContainerThenCreateAndStartNewOneAsImageIdsDontMatch() throws DockerException, IOException {
        when(containerInspectResponseMock.getImageId()).thenReturn("A Different Image Id");

        testObj.start();

        verify(removeContainerCmdMock).exec();
        verify(createContainerCmdMock).exec();
        verify(startContainerCmdMock).exec();
    }

    @Test
    public void stopARunningContainer() {
        when(repoMock.findContainers(idMock, false)).thenReturn(Arrays.asList(containerMock));
        when(stopContainerCmdMock.withTimeout(1)).thenReturn(stopContainerCmdMock);

        testObj.stop();

        verify(stopContainerCmdMock).exec();
    }

    @Test
    public void logsLoadedPlugin() throws Exception {
        verify(logger).info("Loaded " + TestPlugin.class + " plugin");
    }

    @Test
    public void pluginStarted() throws Exception {
        TestPlugin testObjPlugin = testObj.getPlugin(TestPlugin.class);

        assertNull(testObjPlugin.lastStarted());

        testObj.start();

        assertEquals("idMock", testObjPlugin.lastStarted().toString());
    }

    @Test
    public void pluginStopped() throws Exception {
        when(repoMock.findContainers(idMock, false)).thenReturn(Arrays.asList(containerMock));
        TestPlugin testObjPlugin = testObj.getPlugin(TestPlugin.class);

        assertNull(testObjPlugin.lastStopped());

        testObj.stop();

        assertEquals("idMock", testObjPlugin.lastStopped().toString());
    }

    @Test
    public void buildImage() {
        testObj.build(idMock);

        verify(dockerMock).tagImageCmd(IMAGE_ID, IMAGE_NAME, TAG_NAME);
    }

    @Test
    public void buildImageWithRegistryAndPort() {
        String repositoryWithRegistryAndPort = "my.registry.com:5000/mynamespace/myrepository";

        when(confMock.getTags()).thenReturn(Arrays.asList(repositoryWithRegistryAndPort + ":" + TAG_NAME));
        when(tagImageCmdMock.withForce()).thenReturn(tagImageCmdMock);

        testObj.build(idMock);

        verify(dockerMock).tagImageCmd(IMAGE_ID, repositoryWithRegistryAndPort, TAG_NAME);
    }

    @Test
    public void pushImage() {
        testObj.push();

        verify(dockerMock).pushImageCmd(IMAGE_NAME);
    }

    @Test
    public void pushImageWithRegistryAndPort() {
        String repositoryWithRegistryAndPort = "my.registry.com:5000/mynamespace/myrepository";

        when(repoMock.tag(idMock)).thenReturn(repositoryWithRegistryAndPort + ":" + TAG_NAME);

        testObj.push();

        verify(dockerMock).pushImageCmd(repositoryWithRegistryAndPort);
    }
}
