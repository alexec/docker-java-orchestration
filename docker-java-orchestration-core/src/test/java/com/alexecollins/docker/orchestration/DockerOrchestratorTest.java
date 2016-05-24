package com.alexecollins.docker.orchestration;


import com.alexecollins.docker.orchestration.model.BuildFlag;
import com.alexecollins.docker.orchestration.model.Conf;
import com.alexecollins.docker.orchestration.model.ContainerConf;
import com.alexecollins.docker.orchestration.model.HealthChecks;
import com.alexecollins.docker.orchestration.model.Id;
import com.alexecollins.docker.orchestration.model.Link;
import com.alexecollins.docker.orchestration.model.LogPattern;
import com.alexecollins.docker.orchestration.model.VolumeFrom;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Identifier;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.PushResponseItem;
import com.github.dockerjava.api.model.Repository;
import com.github.dockerjava.api.model.StreamType;
import com.google.common.collect.Lists;
import org.apache.commons.lang.time.StopWatch;
import org.glassfish.jersey.client.ClientResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DockerOrchestratorTest {

    private static final String EXTRA_HOST = "foo:127.0.0.1";
    private static final String IMAGE_NAME = "theImage";
    private static final String IMAGE_ID = "imageId";
    private static final String CONTAINER_NAME = "theContainer";
    private static final String CONTAINER_ID = "containerId";
    private static final String TAG_NAME = "test-tag";
    private static final String IP_ADDRESS = "127.0.0.1";
    private static final File SAVE_DIR = new File(System.getProperty("java.io.tmpdir"));
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    @Mock
    private Logger logger;
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
    private InspectContainerResponse.NetworkSettings networkSettingsMock;
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
    private RemoveImageCmd removeImageCmdMock;
    @Mock
    private RemoveContainerCmd removeContainerCmdMock;
    @Mock
    private StopContainerCmd stopContainerCmdMock;
    @Mock
    private CopyFileFromContainerCmd copyFileFromContainerMock;
    @Mock
    private TagImageCmd tagImageCmdMock;
    @Mock
    private SaveImageCmd saveImageCmd;
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
    @Mock
    private InputStream saveInputStream;
    @Mock
    private InputStream tarInputStream;

    private DockerOrchestrator testObj;

    @Before
    public void setup() throws DockerException, IOException {
        testObj = new DockerOrchestrator(
                dockerMock,
                repoMock,
                fileOrchestratorMock,
                EnumSet.noneOf(BuildFlag.class),
                logger,
                tailFactoryMock,
                dockerfileValidator,
                definitionFilter,
                false);

        when(repoMock.src(idMock)).thenReturn(srcFileMock);
        when(repoMock.conf(idMock)).thenReturn(confMock);
        when(repoMock.tag(idMock)).thenReturn(IMAGE_NAME);
        when(repoMock.tags(idMock)).thenReturn(Collections.singletonList(IMAGE_NAME));
        when(repoMock.containerName(idMock)).thenReturn(CONTAINER_NAME);
        when(repoMock.imageName(idMock)).thenReturn(IMAGE_NAME);

        when(confMock.getLinks()).thenReturn(new ArrayList<Link>());
        when(confMock.getContainer()).thenReturn(new ContainerConf());
        HealthChecks healthChecks = mock(HealthChecks.class);
        when(confMock.getHealthChecks()).thenReturn(healthChecks);
        String tag = IMAGE_NAME + ":" + TAG_NAME;
        when(confMock.getTag()).thenReturn(tag);
        when(confMock.getTags()).thenReturn(Collections.singletonList(tag));
        when(confMock.isEnabled()).thenReturn(true);
        final List<String> extraHosts = new ArrayList<>();
        extraHosts.add(EXTRA_HOST);
        when(confMock.getExtraHosts()).thenReturn(extraHosts);
        when(confMock.getVolumesFrom()).thenReturn(new ArrayList<VolumeFrom>());

        when(containerMock.getId()).thenReturn(CONTAINER_ID);
        when(containerMock.getNames()).thenReturn(new String[0]);

        when(fileOrchestratorMock.prepare(idMock, srcFileMock, confMock)).thenReturn(fileMock);

        when(repoMock.ids(false)).thenReturn(Collections.singletonList(idMock));
        when(repoMock.ids(true)).thenReturn(Collections.singletonList(idMock));
        when(repoMock.tag(any(Id.class))).thenReturn(IMAGE_NAME + ":" + TAG_NAME);

        when(dockerMock.removeImageCmd(anyString())).thenReturn(removeImageCmdMock);
        when(removeImageCmdMock.withForce()).thenReturn(removeImageCmdMock);

        when(dockerMock.buildImageCmd(eq(fileMock))).thenReturn(buildImageCmdMock);
        when(buildImageCmdMock.withRemove(anyBoolean())).thenReturn(buildImageCmdMock);
        when(buildImageCmdMock.withTag(any(String.class))).thenReturn(buildImageCmdMock);
        when(buildImageCmdMock.withNoCache(anyBoolean())).thenReturn(buildImageCmdMock);
        when(buildImageCmdMock.withQuiet(anyBoolean())).thenReturn(buildImageCmdMock);
        when(buildImageCmdMock.withPull(anyBoolean())).thenReturn(buildImageCmdMock);
        when(buildImageCmdMock.exec(any(ResultCallback.class))).thenAnswer(
                new Answer<Object>() {
                    @Override
                    public Object answer(final InvocationOnMock invocation) throws Throwable {
                        final ResultCallback o = (ResultCallback) invocation.getArguments()[0];

                        final BuildResponseItem item = new BuildResponseItem() {
                            @Override
                            public String getStream() {
                                return "Successfully built imageId";
                            }
                        };
                        //noinspection unchecked
                        o.onNext(item);

                        o.onComplete();
                        return o;
                    }
                });

                when(dockerMock.createContainerCmd(IMAGE_ID)).thenReturn(createContainerCmdMock);
        when(createContainerCmdMock.exec()).thenReturn(createContainerResponse);
        when(createContainerCmdMock.withName(eq(CONTAINER_NAME))).thenReturn(createContainerCmdMock);

        when(createContainerResponse.getId()).thenReturn(CONTAINER_ID);

        when(dockerMock.copyFileFromContainerCmd(any(String.class), any(String.class))).thenReturn(copyFileFromContainerMock);
        when(copyFileFromContainerMock.exec()).thenReturn(tarInputStream);
        when(copyFileFromContainerMock.withContainerId(CONTAINER_ID)).thenReturn(copyFileFromContainerMock);
        when(copyFileFromContainerMock.withResource(any(String.class))).thenReturn(copyFileFromContainerMock);
        when(copyFileFromContainerMock.withHostPath(any(String.class))).thenReturn(copyFileFromContainerMock);
        when(copyFileFromContainerMock.getContainerId()).thenReturn(CONTAINER_ID);
        when(tarInputStream.read(any(byte[].class), anyInt(), anyInt())).thenReturn(-1);

        when(dockerMock.startContainerCmd(CONTAINER_ID)).thenReturn(startContainerCmdMock);
        when(dockerMock.stopContainerCmd(CONTAINER_ID)).thenReturn(stopContainerCmdMock);
        when(dockerMock.removeContainerCmd(CONTAINER_ID)).thenReturn(removeContainerCmdMock);
        when(dockerMock.listImagesCmd()).thenReturn(listImagesCmdMock);
        when(removeContainerCmdMock.withForce()).thenReturn(removeContainerCmdMock);
        when(removeContainerCmdMock.withRemoveVolumes(true)).thenReturn(removeContainerCmdMock);

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
        when(containerInspectResponseMock.getNetworkSettings()).thenReturn(networkSettingsMock);
        when(networkSettingsMock.getIpAddress()).thenReturn(IP_ADDRESS);

        when(dockerMock.tagImageCmd(anyString(), anyString(), anyString())).thenReturn(tagImageCmdMock);
        when(tagImageCmdMock.withForce()).thenReturn(tagImageCmdMock);

        when(dockerMock.pushImageCmd(anyString())).thenReturn(pushImageCmd);
        when(pushImageCmd.withAuthConfig(any(AuthConfig.class))).thenReturn(pushImageCmd);
        when(pushImageCmd.exec(any(ResultCallback.class))).thenAnswer(
                new Answer<ResultCallback<PushResponseItem>>() {

                    private final LinkedList<PushResponseItem> pushResponses = Lists.newLinkedList();

                    {

                        final String result = "{\"status\":\"The push refers to a repository [docker.io/pushtechnology/daas-management] (len: 2)\"}";
                        final String error = "{\"errorDetail\":{\"message\":\"Received unexpected HTTP status: 500 Internal Server Error\"},\"error\":\"Received unexpected HTTP status: 500 Internal Server Error\"}";

                        final ObjectMapper mapper = new ObjectMapper();
                        pushResponses.add(mapper.readValue(result, PushResponseItem.class));
                        pushResponses.add(mapper.readValue(error, PushResponseItem.class));
                    }

                    @Override
                    public ResultCallback<PushResponseItem> answer(final InvocationOnMock invocation) throws Throwable {
                        @SuppressWarnings("unchecked") ResultCallback<PushResponseItem> callback = (ResultCallback<PushResponseItem>) invocation.getArguments()[0];

                        callback.onNext(pushResponses.removeFirst());
                        callback.onNext(pushResponses.removeFirst());
                        callback.onComplete();

                        return callback;
                    }
                });

        when(definitionFilter.test(any(Id.class), any(Conf.class))).thenReturn(true);
        when(tailFactoryMock.newTail(any(DockerClient.class), any(Container.class), any(Logger.class))).thenReturn(tailMock);
        when(dockerMock.saveImageCmd(anyString())).thenReturn(saveImageCmd);
        when(saveImageCmd.exec()).thenReturn(saveInputStream);
        when(saveInputStream.read(any(byte[].class))).thenReturn(-1);
    }

    @After
    public void tearDown() {
        backgroundExecutor.shutdown();
    }

    @Test
    public void cleaningForcesImageRemoval() throws Exception {

        testObj.clean();

        verify(removeImageCmdMock).withForce();
        verify(removeImageCmdMock).exec();
    }

    @Test
    public void createAndStartNewContainer() throws DockerException, IOException {

        mockRunningIdMock();

        testObj.start();

        verify(createContainerCmdMock).exec();
        verify(createContainerCmdMock).withExtraHosts(EXTRA_HOST);
        verify(startContainerCmdMock).exec();
    }

    private void mockRunningIdMock() {
        final Container container = mock(Container.class);
        when(container.getImage()).thenReturn("idMock");
        when(container.getNames()).thenReturn(new String[0]);
        when(listContainersCmdMock.exec()).thenReturn(Collections.<Container>emptyList()).thenReturn(Collections.singletonList(container));
        when(repoMock.imageName(any(Id.class))).thenReturn("idMock");
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
    public void copyResourceFromContainer() throws DockerException, IOException {
        when(listContainersCmdMockOnlyRunning.exec()).thenReturn(Collections.<Container>emptyList());
        testObj.copy("a resource", "a path");

        verify(copyFileFromContainerMock, times(1)).exec();
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
        try {
            testObj.push();
            fail("Exception expected");
        }
        catch (OrchestrationException oe) {
            // need to get here
        }
        verify(dockerMock).pushImageCmd(IMAGE_NAME);
        verify(pushImageCmd, never()).withTag(anyString());
    }

    @Test
    public void pushImageWithRegistryAndPort() {
        final Identifier identifier = new Identifier(new Repository("my.registry.com:5000/mynamespace/myrepository"), TAG_NAME);

        when(repoMock.tags(idMock)).thenReturn(Collections.singletonList(identifier.repository.name + ":" + TAG_NAME));
        try {
            testObj.push();
            fail("Exception expected");
        }
        catch (OrchestrationException oe) {
            // need to get here
        }

        verify(dockerMock).pushImageCmd(identifier.repository.name);
        verify(pushImageCmd).withTag(TAG_NAME);
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
        mockRunningIdMock();

        testObj.start();

        verify(createContainerCmdMock).withPrivileged(true);
    }

    @Test
    public void networkModeConfigurationStartsContainerInSpecifiedNetworkMode() {
        when(confMock.getNetworkMode()).thenReturn("host");
        mockRunningIdMock();

        testObj.start();

        verify(createContainerCmdMock).withNetworkMode("host");

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
        when(confMock.getHealthChecks().getLogPatterns()).thenReturn(Collections.singletonList(new LogPattern("^Foo$")));
        final LogContainerCmd cmd = mockLogContainerCmd("Foo");

        testObj.start();

        verify(cmd, times(1)).exec(any(ResultCallback.class));
    }

    @Test
    public void testWaitForLineFailEndOfInput() {
        when(confMock.getHealthChecks().getLogPatterns()).thenReturn(Collections.singletonList(new LogPattern("^Foo$")));
        mockLogContainerCmd("Bar");

        try {
            testObj.start();
            fail();
        } catch (OrchestrationException e) {
            assertThat(e.getMessage(), endsWith(String.format("%s's log ended before [\"^Foo$\"] appeared in output", idMock)));
        }
    }

    @Test
    public void patternsAreLoggedBothWhenRequestAndFound() {
        List<LogPattern> logPatterns = Collections.singletonList(new LogPattern("^Foo$"));
        when(confMock.getHealthChecks().getLogPatterns()).thenReturn(logPatterns);
        mockLogContainerCmd("Foo");

        testObj.start();

        verify(logger).info(eq("Waiting for {} to appear in output"), eq("[\"^Foo$\"]"));
        // assume that as we only have one pattern, it will be the same
        verify(logger).info(eq("Waited {} for \"{}\""), any(StopWatch.class), eq("^Foo$"));
    }

    @Test
    public void timeOutWaitingForLogs() {
        // pattern are in order of timeout here
        LogPattern firstLogPattern = new LogPattern("^Foo$");
        firstLogPattern.setTimeout(0);
        LogPattern secondLogPattern = new LogPattern("^Bar$");
        // here we have reverse order
        when(confMock.getHealthChecks().getLogPatterns()).thenReturn(Arrays.asList(secondLogPattern, firstLogPattern));
        mockLogContainerCmd("Bar");

        try {
            testObj.start();
            fail();
        } catch (OrchestrationException e) {
            assertThat(e.getMessage(), endsWith(String.format("timeout after 0 while waiting for \"%s\" in %s's logs", firstLogPattern.getPattern(), idMock)));
        }

        verify(logger).info(eq("Waiting for {} to appear in output"), eq("[\"^Foo$\", \"^Bar$\"]"));
        verify(logger).info(eq("Waited {} for \"{}\""), any(StopWatch.class), eq("^Bar$"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotExposeIPAddressWhenContainerIsNotExposed() {
        when(confMock.isExposeContainerIp()).thenReturn(false);
        testObj.getIPAddress(idMock);
        fail("Expected exception to be thrown");
    }

    @Test
    public void shouldExposeIPAddressFromDockeExec() {
        when(confMock.isExposeContainerIp()).thenReturn(true);
        String result = testObj.getIPAddress(idMock);
        assertEquals(IP_ADDRESS, result);
        verify(inspectContainerCmdMock, times(1)).exec();
    }

    private LogContainerCmd mockLogContainerCmd(final String containerOutput) {
        final LogContainerCmd cmd = mock(LogContainerCmd.class);

        when(cmd.withStdErr()).thenReturn(cmd);
        when(cmd.withStdOut()).thenReturn(cmd);
        when(cmd.withTailAll()).thenReturn(cmd);
        when(cmd.withFollowStream()).thenReturn(cmd);
        when(cmd.withTimestamps()).thenReturn(cmd);

        when(cmd.exec(any(ResultCallback.class))).thenAnswer(
                new Answer<Object>() {
                    @Override
                    public Object answer(final InvocationOnMock invocation) throws Throwable {
                        @SuppressWarnings("unchecked") final ResultCallback<Frame> callback = (ResultCallback<Frame>) invocation.getArguments()[0];

                        final Future<Void> result = backgroundExecutor.submit(new Callable<Void>() {

                            @Override
                            public Void call() throws Exception {
                                callback.onNext(new Frame(StreamType.STDOUT, containerOutput.getBytes()));
                                callback.onComplete();
                                return null;
                            }
                        });

                        try {
                            result.get();
                        } catch (ExecutionException e) {
                            throw e.getCause();
                        }
                        return callback;
                    }
                }
        );

        when(dockerMock.logContainerCmd(containerMock.getId())).thenReturn(cmd);
        return cmd;
    }

    @Test
    public void saveTarShouldInvokeDockerAn() throws Exception {

        // when
        Map<Id, File> saved = testObj.save(SAVE_DIR, false);

        // then
        File expectedFile = new File(SAVE_DIR, idMock + ".tar");
        assertEquals(Collections.singletonMap(idMock, expectedFile), saved);
    }

    @Test
    public void saveShouldInvokeDockerAndLog() throws Exception {

        // when
        Map<Id, File> saved = testObj.save(SAVE_DIR, true);

        // then
        File expectedFile = new File(SAVE_DIR, idMock + ".tar.gz");
        assertEquals(Collections.singletonMap(idMock, expectedFile), saved);

        verify(logger).info("Saving {} as {}", idMock, expectedFile);
        verify(logger).warn("Image does NOT have tag. Saving using image ID. Export will be missing 'repositories' data.");
        verify(logger).info("Saving image {}", imageMock.getId());
        verify(dockerMock).saveImageCmd(IMAGE_ID);
    }


    @Test
    public void taggedImageSaveShouldInvokeDockerAndLog() throws Exception {

        // given
        when(confMock.hasTag()).thenReturn(true);

        // when
        Map<Id, File> saved = testObj.save(SAVE_DIR, true);

        // then
        File expectedFile = new File(SAVE_DIR, idMock + ".tar.gz");
        assertEquals(Collections.singletonMap(idMock, expectedFile), saved);

        verify(logger).info("Saving {} as {}", idMock, expectedFile);
        verify(logger).info("Saving image {}", confMock.getTag());
        verify(dockerMock).saveImageCmd(confMock.getTag());
    }
}
