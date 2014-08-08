package com.alexecollins.docker.orchestration;


import com.alexecollins.docker.orchestration.model.Conf;
import com.alexecollins.docker.orchestration.model.Credentials;
import com.alexecollins.docker.orchestration.model.HealthChecks;
import com.alexecollins.docker.orchestration.model.Id;
import com.kpelykh.docker.client.DockerClient;
import com.kpelykh.docker.client.DockerException;
import com.kpelykh.docker.client.model.*;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

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
    @Mock private Credentials credentialsMock;
    @Mock private Id idMock;
    @Mock private FileOrchestrator fileOrchestratorMock;
    @Mock private ClientResponse clientResponseMock;
    @Mock private Conf confMock;
    @Mock private ContainerCreateResponse containerCreateResponseMock;
    @Mock private ContainerConfig containerConfigMock;
    @Mock private Container containerMock;
    @Mock private ContainerInspectResponse containerInspectResponseMock;

    private DockerOrchestrator testObj;

    @Before
    public void setup () throws DockerException, IOException {
        testObj = new DockerOrchestrator(dockerMock, repoMock, fileOrchestratorMock, credentialsMock);

        when(repoMock.src(idMock)).thenReturn(srcFileMock);
        when(repoMock.conf(idMock)).thenReturn(confMock);
        when(repoMock.imageName(idMock)).thenReturn(IMAGE_NAME);
        when(repoMock.getImageId(idMock)).thenReturn(IMAGE_NAME);
        when(repoMock.containerName(idMock)).thenReturn(CONTAINER_NAME);

        when(confMock.getLinks()).thenReturn(new ArrayList<Id>());
	    when(confMock.getHealthChecks()).thenReturn(new HealthChecks());

        when(fileOrchestratorMock.prepare(idMock, srcFileMock, confMock)).thenReturn(fileMock);

        when(repoMock.ids(false)).thenReturn(Arrays.asList(idMock));
        when(dockerMock.build(eq(fileMock), eq(IMAGE_NAME), any(Set.class))).thenReturn(clientResponseMock);
        when(dockerMock.createContainer(any(ContainerConfig.class), eq(CONTAINER_NAME))).thenReturn(containerCreateResponseMock);
        when(clientResponseMock.getEntityInputStream()).thenReturn(IOUtils.toInputStream("Successfully built"));
        when(containerCreateResponseMock.getId()).thenReturn(CONTAINER_ID);
    }


    @Test
    public void createAndStartNewContainer() throws DockerException, IOException {

        when(repoMock.imageExists(idMock)).thenReturn(false, true);

        testObj.start();

        verify(dockerMock).createContainer(any(ContainerConfig.class), eq(CONTAINER_NAME));
        verify(dockerMock).startContainer(eq(CONTAINER_ID), any(HostConfig.class));
    }

    @Test
    public void startExistingContainerAsImageIdsMatch() throws DockerException, IOException {

        when(repoMock.getImageId(idMock)).thenReturn(IMAGE_ID);
        when(repoMock.findContainer(idMock)).thenReturn(containerMock);
        when(containerMock.getId()).thenReturn(CONTAINER_ID);
        when(dockerMock.inspectContainer(CONTAINER_ID)).thenReturn(containerInspectResponseMock);
        when(containerInspectResponseMock.getImage()).thenReturn(IMAGE_ID);

        testObj.start();

        verify(dockerMock, times(0)).createContainer(any(ContainerConfig.class), eq(CONTAINER_NAME));
        verify(dockerMock).startContainer(eq(CONTAINER_ID), any(HostConfig.class));
    }

    @Test
    public void containerIsAlreadyRunning() throws DockerException, IOException {

        when(repoMock.getImageId(idMock)).thenReturn(IMAGE_ID);
        when(repoMock.findContainer(idMock)).thenReturn(containerMock);
        when(containerMock.getId()).thenReturn(CONTAINER_ID);
        when(dockerMock.inspectContainer(CONTAINER_ID)).thenReturn(containerInspectResponseMock);
        when(containerInspectResponseMock.getImage()).thenReturn(IMAGE_ID);
        when(dockerMock.listContainers(false)).thenReturn(Arrays.asList(containerMock));

        testObj.start();

        verify(dockerMock, times(0)).createContainer(any(ContainerConfig.class), eq(CONTAINER_NAME));
        verify(dockerMock, times(0)).startContainer(eq(CONTAINER_ID), any(HostConfig.class));
    }

    @Test
    public void removeExistingContainerThenCreateAndStartNewOneAsImageIdsDontMatch() throws DockerException, IOException {

        when(repoMock.getImageId(idMock)).thenReturn(IMAGE_ID);
        when(repoMock.findContainer(idMock)).thenReturn(containerMock);
        when(containerMock.getId()).thenReturn(CONTAINER_ID);
        when(dockerMock.inspectContainer(CONTAINER_ID)).thenReturn(containerInspectResponseMock);
        when(containerInspectResponseMock.getImage()).thenReturn("A Different Image Id");

        testObj.start();

        verify(dockerMock).removeContainer(eq(CONTAINER_ID));
        verify(dockerMock).createContainer(any(ContainerConfig.class), eq(CONTAINER_NAME));
        verify(dockerMock).startContainer(eq(CONTAINER_ID), any(HostConfig.class));
    }
}
