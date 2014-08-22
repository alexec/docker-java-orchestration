package com.alexecollins.docker.orchestration;


import com.alexecollins.docker.orchestration.model.Conf;
import com.alexecollins.docker.orchestration.model.HealthChecks;
import com.alexecollins.docker.orchestration.model.Id;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerConfig;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.client.ClientResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DockerOrchestratorUTest {

    private static final String IMAGE_NAME = "theImage";
    private static final String IMAGE_ID = "imageId";

    private static final String CONTAINER_NAME = "theContainer";
    private static final String CONTAINER_ID = "containerId";

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

    private DockerOrchestrator testObj;


    @Before
    public void setup () throws DockerException, IOException {
        testObj = new DockerOrchestrator(dockerMock, repoMock, fileOrchestratorMock);

        when(repoMock.src(idMock)).thenReturn(srcFileMock);
        when(repoMock.conf(idMock)).thenReturn(confMock);
        when(repoMock.imageName(idMock)).thenReturn(IMAGE_NAME);
        when(repoMock.getImageId(idMock)).thenReturn(IMAGE_ID);
        when(repoMock.containerName(idMock)).thenReturn(CONTAINER_NAME);

        when(confMock.getLinks()).thenReturn(new ArrayList<Id>());
	    when(confMock.getHealthChecks()).thenReturn(new HealthChecks());

        when(repoMock.getImageId(idMock)).thenReturn(IMAGE_ID);
        when(repoMock.findContainer(idMock)).thenReturn(containerMock);
        when(containerMock.getId()).thenReturn(CONTAINER_ID);

        when(fileOrchestratorMock.prepare(idMock, srcFileMock, confMock)).thenReturn(fileMock);

        when(repoMock.ids(false)).thenReturn(Arrays.asList(idMock));

        when(dockerMock.buildImageCmd(eq(fileMock))).thenReturn(buildImageCmdMock);
        when(buildImageCmdMock.exec()).thenReturn(IOUtils.toInputStream("Successfully built"));

        when(dockerMock.createContainerCmd(IMAGE_ID)).thenReturn(createContainerCmdMock);
        when(createContainerCmdMock.exec()).thenReturn(createContainerResponse);
        when(createContainerCmdMock.withName(eq(CONTAINER_NAME))).thenReturn(createContainerCmdMock);

        when(createContainerResponse.getId()).thenReturn(CONTAINER_ID);

        when(dockerMock.startContainerCmd(CONTAINER_ID)).thenReturn(startContainerCmdMock);

        when(dockerMock.removeContainerCmd(CONTAINER_ID)).thenReturn(removeContainerCmdMock);

        when(dockerMock.listContainersCmd()).thenReturn(listContainersCmdMockOnlyRunning);
        when(listContainersCmdMockOnlyRunning.withShowAll(false)).thenReturn(listContainersCmdMockOnlyRunning);
        when(listContainersCmdMockOnlyRunning.exec()).thenReturn(Collections.EMPTY_LIST);

        when(dockerMock.inspectContainerCmd(CONTAINER_ID)).thenReturn(inspectContainerCmdMock);
        when(inspectContainerCmdMock.exec()).thenReturn(containerInspectResponseMock);
        when(containerInspectResponseMock.getImageId()).thenReturn(IMAGE_ID);

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
}
