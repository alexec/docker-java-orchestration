package com.alexecollins.docker.orchestration;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.alexecollins.docker.orchestration.model.BuildFlag;
import com.alexecollins.docker.orchestration.model.Conf;
import com.alexecollins.docker.orchestration.model.ContainerConf;
import com.alexecollins.docker.orchestration.model.HealthChecks;
import com.alexecollins.docker.orchestration.model.Id;
import com.alexecollins.docker.orchestration.model.Link;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.command.PushImageCmd;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.StopContainerCmd;
import com.github.dockerjava.api.command.TagImageCmd;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.PushEventStreamItem;
import com.github.dockerjava.jaxrs.BuildImageCmdExec;
import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.client.ClientResponse;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DockerOrchestratorTest {

    public static final String EXTRA_HOST = "foo:127.0.0.1";
    private static final String IMAGE_NAME = "theImage";
    private static final String IMAGE_ID = "imageId";
    private static final String CONTAINER_NAME = "theContainer";
    private static final String CONTAINER_ID = "containerId";
    private static final String TAG_NAME = "test-tag";
    private final static Logger LOGGER = (Logger) LoggerFactory.getLogger(DockerOrchestrator.class);
    @SuppressWarnings("unchecked")
    private final Appender<ILoggingEvent> appender = mock(Appender.class);
    private final ArgumentCaptor<ILoggingEvent> captor = ArgumentCaptor.forClass(ILoggingEvent.class);
    @Mock
    private DockerClient dockerMock;
    @Mock
    private Repo repoMock;
    @Mock
    private File fileMock;
    @Mock
    private File srcFileMock;
    @Mock
    private Id idMock;
    @Mock
    private FileOrchestrator fileOrchestratorMock;
    @Mock
    private ClientResponse clientResponseMock;
    @Mock
    private Conf confMock;
    @Mock
    private CreateContainerResponse createContainerResponse;
    @Mock
    private ContainerConfig containerConfigMock;
    @Mock
    private Container containerMock;
    @Mock
    private Image imageMock;
    @Mock
    private InspectContainerResponse containerInspectResponseMock;
    @Mock
    private BuildImageCmd buildImageCmdMock;
    @Mock
    private CreateContainerCmd createContainerCmdMock;
    @Mock
    private StartContainerCmd startContainerCmdMock;
    @Mock
    private InspectContainerCmd inspectContainerCmdMock;
    @Mock
    private ListContainersCmd listContainersCmdMockOnlyRunning;
    @Mock
    private ListContainersCmd listContainersCmdMock;
    @Mock
    private RemoveContainerCmd removeContainerCmdMock;
    @Mock
    private StopContainerCmd stopContainerCmdMock;
    @Mock
    private TagImageCmd tagImageCmdMock;
    @Mock
    private PushImageCmd pushImageCmd;
    @Mock
    private ListImagesCmd listImagesCmdMock;
    @Mock
    private DockerfileValidator dockerfileValidator;
    @Mock
    private DefinitionFilter definitionFilter;
    @Mock
    private Tail tailMock;
    @Mock
    private TailFactory tailFactoryMock;
    private DockerOrchestrator testObj;

    private static TypeSafeMatcher<ILoggingEvent> loggedMessage(final String message) {
        return new TypeSafeMatcher<ILoggingEvent>() {
            @Override
            protected boolean matchesSafely(ILoggingEvent event) {
                return event.getFormattedMessage().contains(message);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(String.format("message = <%s>", message));
            }
        };
    }

    @Before
    public void setup() throws DockerException, IOException {
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        LOGGER.setLevel(Level.INFO);
        LOGGER.addAppender(appender);

        testObj = new DockerOrchestrator(
                dockerMock,
                repoMock,
                fileOrchestratorMock,
                EnumSet.noneOf(BuildFlag.class),
                LOGGER,
                tailFactoryMock,
                dockerfileValidator,
                definitionFilter,
                false);

        when(repoMock.src(idMock)).thenReturn(srcFileMock);
        when(repoMock.conf(idMock)).thenReturn(confMock);
        when(repoMock.tag(idMock)).thenReturn(IMAGE_NAME);
        when(repoMock.containerName(idMock)).thenReturn(CONTAINER_NAME);
        when(repoMock.imageName(idMock)).thenReturn(IMAGE_NAME);

        when(confMock.getLinks()).thenReturn(new ArrayList<Link>());
        when(confMock.getContainer()).thenReturn(new ContainerConf());
        when(confMock.getHealthChecks()).thenReturn(new HealthChecks());
        when(confMock.getTags()).thenReturn(Collections.singletonList(IMAGE_NAME + ":" + TAG_NAME));
        when(confMock.isEnabled()).thenReturn(true);
        final List<String> extraHosts = new ArrayList<>();
        extraHosts.add(EXTRA_HOST);
        when(confMock.getExtraHosts()).thenReturn(extraHosts);

        when(containerMock.getId()).thenReturn(CONTAINER_ID);
        when(containerMock.getNames()).thenReturn(new String[0]);

        when(fileOrchestratorMock.prepare(idMock, srcFileMock, confMock)).thenReturn(fileMock);

        when(repoMock.ids(false)).thenReturn(Collections.singletonList(idMock));
        when(repoMock.ids(true)).thenReturn(Collections.singletonList(idMock));
        when(repoMock.tag(any(Id.class))).thenReturn(IMAGE_NAME + ":" + TAG_NAME);

        when(dockerMock.buildImageCmd(eq(fileMock))).thenReturn(buildImageCmdMock);
        when(buildImageCmdMock.withRemove(anyBoolean())).thenReturn(buildImageCmdMock);
        when(buildImageCmdMock.withTag(any(String.class))).thenReturn(buildImageCmdMock);
        when(buildImageCmdMock.withNoCache(anyBoolean())).thenReturn(buildImageCmdMock);
        when(buildImageCmdMock.withQuiet(anyBoolean())).thenReturn(buildImageCmdMock);
        when(buildImageCmdMock.exec()).thenReturn(new BuildImageCmdExec.ResponseImpl(IOUtils.toInputStream("Successfully built")));

        when(dockerMock.createContainerCmd(IMAGE_ID)).thenReturn(createContainerCmdMock);
        when(createContainerCmdMock.exec()).thenReturn(createContainerResponse);
        when(createContainerCmdMock.withName(eq(CONTAINER_NAME))).thenReturn(createContainerCmdMock);

        when(createContainerResponse.getId()).thenReturn(CONTAINER_ID);

        when(dockerMock.startContainerCmd(CONTAINER_ID)).thenReturn(startContainerCmdMock);
        when(dockerMock.stopContainerCmd(CONTAINER_ID)).thenReturn(stopContainerCmdMock);
        when(dockerMock.removeContainerCmd(CONTAINER_ID)).thenReturn(removeContainerCmdMock);
        when(dockerMock.listImagesCmd()).thenReturn(listImagesCmdMock);
        when(removeContainerCmdMock.withForce()).thenReturn(removeContainerCmdMock);

        when(listImagesCmdMock.exec()).thenReturn(Collections.singletonList(imageMock));

        when(imageMock.getId()).thenReturn(IMAGE_ID);
        when(imageMock.getRepoTags()).thenReturn(new String[]{IMAGE_NAME + ":" + TAG_NAME});

        when(dockerMock.listContainersCmd()).thenReturn(listContainersCmdMock);
        when(listContainersCmdMock.withShowAll(true)).thenReturn(listContainersCmdMock);
        when(listContainersCmdMock.withShowAll(false)).thenReturn(listContainersCmdMockOnlyRunning);
        when(listContainersCmdMock.exec()).thenReturn(Collections.singletonList(containerMock));
        when(listContainersCmdMockOnlyRunning.exec()).thenReturn(Collections.singletonList(containerMock));
        when(containerMock.getImage()).thenReturn(DockerOrchestratorTest.IMAGE_NAME);

        when(stopContainerCmdMock.withTimeout(anyInt())).thenReturn(stopContainerCmdMock);

        when(dockerMock.inspectContainerCmd(CONTAINER_ID)).thenReturn(inspectContainerCmdMock);
        when(inspectContainerCmdMock.exec()).thenReturn(containerInspectResponseMock);
        when(containerInspectResponseMock.getImageId()).thenReturn(IMAGE_ID);

        when(dockerMock.tagImageCmd(anyString(), anyString(), anyString())).thenReturn(tagImageCmdMock);
        when(tagImageCmdMock.withForce()).thenReturn(tagImageCmdMock);

        when(dockerMock.pushImageCmd(anyString())).thenReturn(pushImageCmd);
        when(pushImageCmd.withAuthConfig(any(AuthConfig.class))).thenReturn(pushImageCmd);
        when(pushImageCmd.exec()).thenReturn(new PushImageCmd.Response() {
            private final InputStream proxy = IOUtils.toInputStream("{\"status\":\"The push refers to...\"}");

            @Override
            public int read() throws IOException {
                return proxy.read();
            }

            @Override
            public Iterable<PushEventStreamItem> getItems() throws IOException {
                return null;
            }
        });

        when(definitionFilter.test(any(Id.class), any(Conf.class))).thenReturn(true);
        when(tailFactoryMock.newTail(any(DockerClient.class), any(Container.class), any(Logger.class))).thenReturn(tailMock);
    }

    @Test
    public void createAndStartNewContainer() throws DockerException, IOException {

        when(listContainersCmdMock.exec()).thenReturn(Collections.<Container>emptyList());

        testObj.start();

        verify(createContainerCmdMock).exec();
        verify(createContainerCmdMock).withExtraHosts(EXTRA_HOST);
        verify(startContainerCmdMock).exec();
    }

    @Test
    public void startExistingContainerAsImageIdsMatch() throws DockerException, IOException {
        when(listContainersCmdMockOnlyRunning.exec()).thenReturn(Collections.<Container>emptyList());
        testObj.start();

        verify(createContainerCmdMock, times(0)).exec();
        verify(startContainerCmdMock).exec();
    }

    @Test
    public void containerIsAlreadyRunning() throws DockerException, IOException {
        when(listContainersCmdMock.exec()).thenReturn(Collections.singletonList(containerMock));

        testObj.start();

        verify(createContainerCmdMock, times(0)).exec();
        verify(startContainerCmdMock, times(0)).exec();
    }

    @Test
    public void removeExistingContainerThenCreateAndStartNewOneAsImageIdsDoNotMatch() throws DockerException, IOException {
        when(containerInspectResponseMock.getImageId()).thenReturn("A Different Image Id");

        testObj.start();

        verify(removeContainerCmdMock).exec();
        verify(createContainerCmdMock).exec();
        verify(startContainerCmdMock).exec();
    }

    @Test
    public void stopARunningContainer() {
        when(listContainersCmdMock.exec()).thenReturn(Collections.singletonList(containerMock));
        when(stopContainerCmdMock.withTimeout(1)).thenReturn(stopContainerCmdMock);

        testObj.stop();

        verify(stopContainerCmdMock).exec();
    }

    @Test
    public void logsLoadedPlugin() throws Exception {
        verify(appender, atLeastOnce()).doAppend(captor.capture());
        List<ILoggingEvent> logging = captor.getAllValues();
        assertThat(logging, CoreMatchers.hasItem((loggedMessage("Loaded " + TestPlugin.class + " plugin"))));
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

        when(confMock.getTags()).thenReturn(Collections.singletonList(repositoryWithRegistryAndPort + ":" + TAG_NAME));
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

    @Test
    public void validationDelegatesToDockerfileValidator() throws Exception {

        testObj.validate();
        verify(dockerfileValidator).validate(srcFileMock);

    }

    @Test
    public void filteredDefinitionsAreNotInvoked() throws Exception {
        when(definitionFilter.test(any(Id.class), any(Conf.class))).thenReturn(false);

        testObj.validate();
        testObj.clean();
        testObj.build();
        testObj.start();
        testObj.stop();
        testObj.push();

        verifyNoMoreInteractions(dockerMock);


    }

    @Test
    public void disabledContainerResultsInNoInteraction() throws Exception {
        when(confMock.isEnabled()).thenReturn(false);

        testObj.validate();
        testObj.clean();
        testObj.build();
        testObj.start();
        testObj.stop();
        testObj.push();

        verifyNoMoreInteractions(dockerMock);

    }

    @Test
    public void privilegedConfigurationStartsPrivilegedContainer() throws Exception {

        when(confMock.isPrivileged()).thenReturn(true);
        when(listContainersCmdMock.exec()).thenReturn(Collections.<Container>emptyList());

        testObj.start();

        verify(createContainerCmdMock).withPrivileged(true);
    }

    @Test
    public void namelessContainersAreIgnored() {
        when(containerMock.getNames()).thenReturn(null);
        when(listContainersCmdMock.exec()).thenReturn(Collections.singletonList(containerMock));
        when(stopContainerCmdMock.withTimeout(1)).thenReturn(stopContainerCmdMock);

        testObj.stop();

        verify(listContainersCmdMockOnlyRunning).exec();
        verify(stopContainerCmdMock, times(0)).exec();
    }

    @Test
    public void testWaitForLine() {
        when(confMock.getWaitForLine()).thenReturn("^Foo$");
        final LogContainerCmd cmd = mockLogContainerCmd("Foo");

        testObj.start();

        verify(cmd, times(1)).exec();
    }

    @Test
    public void testWaitForLineFailEndOfInput() {
        when(confMock.getWaitForLine()).thenReturn("^Foo$");
        final LogContainerCmd cmd = mockLogContainerCmd("Bar");

        testObj.start();

        verify(cmd, times(1)).exec();

        verify(appender, atLeastOnce()).doAppend(captor.capture());
        List<ILoggingEvent> logging = captor.getAllValues();
        assertThat(logging, CoreMatchers.hasItem((loggedMessage("Unable to obtain logs from container " + containerMock.getId()
                + ", will continue without waiting"))));
    }

    @Test
    public void testWaitForLineInvalidRegex() {
        when(confMock.getWaitForLine()).thenReturn("^Foo[$");

        try {
            testObj.start();
            fail("Expected exception");
        } catch (OrchestrationException e) {
            assertThat(e.getCause(),instanceOf(PatternSyntaxException.class));
        }
    }

    private LogContainerCmd mockLogContainerCmd(String containerOutput) {
        final LogContainerCmd cmd = mock(LogContainerCmd.class);

        when(cmd.withStdErr()).thenReturn(cmd);
        when(cmd.withStdOut()).thenReturn(cmd);
        when(cmd.withFollowStream()).thenReturn(cmd);
        when(cmd.withTimestamps()).thenReturn(cmd);

        when(cmd.exec()).thenReturn(new ByteArrayInputStream(containerOutput.getBytes(Charsets.UTF_8)));

        when(dockerMock.logContainerCmd(containerMock.getId())).thenReturn(cmd);
        return cmd;
    }

}
