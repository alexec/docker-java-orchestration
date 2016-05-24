package com.alexecollins.docker.orchestration;


import com.alexecollins.docker.orchestration.model.*;
import com.alexecollins.docker.orchestration.plugin.api.Plugin;
import com.alexecollins.docker.orchestration.util.Pinger;
import com.github.dockerjava.api.*;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.api.model.Link;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import com.github.dockerjava.core.command.PushImageResultCallback;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import static java.util.Arrays.asList;

/**
 * Orchestrates multiple Docker containers based on
 */
public class DockerOrchestrator {
    /**
     * @deprecated This will be removed in a future release.
     */
    @Deprecated
    @SuppressWarnings("unused")
    public static final FileFilter DEFAULT_FILTER = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return false;
        }
    };
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(DockerOrchestrator.class);
    private static final String CONTAINER_IP_PATTERN = "__CONTAINER.IP__";

    private final Logger logger;
    private final DockerClient docker;
    private final TailFactory tailFactory;
    private final Repo repo;

    private final FileOrchestrator fileOrchestrator;
    private final Set<BuildFlag> buildFlags;
    private final List<Plugin> plugins = new ArrayList<>();
    private final DockerfileValidator dockerfileValidator;
    private final DefinitionFilter definitionFilter;
    private final boolean permissionErrorTolerant;

    /**
     * @deprecated Please use builder from now on.
     */
    @Deprecated
    public DockerOrchestrator(DockerClient docker, File src, File workDir, File rootDir, String user, String project, FileFilter filter, Properties properties) {
        //noinspection deprecation
        this(docker, src, workDir, rootDir, user, project, filter, properties, EnumSet.noneOf(BuildFlag.class));
    }

    /**
     * @deprecated Please use builder from now on.
     */
    @Deprecated
    public DockerOrchestrator(DockerClient docker, File src, File workDir, File rootDir, String user, String project, FileFilter filter, Properties properties, Set<BuildFlag> buildFlags) {
        this(
                docker,
                new Repo(user, project, src, properties),
                new FileOrchestrator(workDir, rootDir, filter, properties),
                buildFlags,
                DEFAULT_LOGGER,
                TailFactory.DEFAULT,
                new DockerfileValidator(),
                DefinitionFilter.ANY,
                false);
    }

    DockerOrchestrator(DockerClient docker, Repo repo, FileOrchestrator fileOrchestrator, Set<BuildFlag> buildFlags, Logger logger, TailFactory tailFactory, DockerfileValidator dockerfileValidator, DefinitionFilter definitionFilter, boolean permissionErrorTolerant) {
        if (docker == null) {
            throw new IllegalArgumentException("docker is null");
        }
        if (repo == null) {
            throw new IllegalArgumentException("repo is null");
        }
        if (buildFlags == null) {
            throw new IllegalArgumentException("buildFlags is null");
        }
        if (fileOrchestrator == null) {
            throw new IllegalArgumentException("fileOrchestrator is null");
        }
        if (dockerfileValidator == null) {
            throw new IllegalArgumentException("dockerfileValidator is null");
        }
        if (definitionFilter == null) {
            throw new IllegalArgumentException("definitionFilter is null");
        }

        this.docker = docker;
        this.tailFactory = tailFactory;
        this.repo = repo;
        this.fileOrchestrator = fileOrchestrator;
        this.buildFlags = buildFlags;
        this.logger = logger;
        this.dockerfileValidator = dockerfileValidator;
        this.definitionFilter = definitionFilter;
        this.permissionErrorTolerant = permissionErrorTolerant;

        for (Plugin plugin : ServiceLoader.load(Plugin.class)) {
            plugins.add(plugin);
            logger.info("Loaded " + plugin.getClass() + " plugin");
        }
    }

    public static DockerOrchestratorBuilder builder() {
        return new DockerOrchestratorBuilder();
    }

    private static boolean isPermissionError(InternalServerErrorException e) {
        return e.getMessage().contains("operation not permitted");
    }

    private static List<LogPattern> sortedLogPatterns(List<LogPattern> logPatterns) {
        final List<LogPattern> pending = new ArrayList<>(logPatterns);
        Collections.sort(pending, new Comparator<LogPattern>() {
            @Override
            public int compare(LogPattern o1, LogPattern o2) {
                return o1.getTimeout() - o2.getTimeout();
            }
        });
        return pending;
    }

    static String logPatternsToString(List<LogPattern> pending) {
        return Lists.transform(pending, new Function<LogPattern, String>() {
            @Override
            public String apply(LogPattern input) {
                return String.format("\"%s\"", input.getPattern().pattern());
            }
        }).toString();
    }

    private static String buildResponseItemToString(final ResponseItem item) {
        if (item.getStream() != null) {
            return item.getStream();
        }
        if (item.getStatus() != null) {
            if (item.getProgress() != null) {
                return item.getStatus() + ": " + item.getProgress();
            }
            return item.getStatus();
        }

        if (item.getError() != null) {
            return item.getError();
        }

        return item.toString();
    }

    public void clean() {
        clean(CleanFlag.CONTAINER_AND_IMAGE);
    }

    public void cleanContainers() {
        clean(CleanFlag.CONTAINER_ONLY);
    }

    public void clean(final CleanFlag cleanFlag) {
        for (Id id : repo.ids(true)) {
            if (!inclusive(id)) {
                continue;
            }
            stop(id);
            clean(id, cleanFlag);
        }
    }

    private boolean inclusive(Id id) {
        Conf conf = conf(id);
        if (!definitionFilter.test(id, conf)) {
            logger.info("Not including " + id + ", filtered out");
            return false;
        }
        if (!conf.isEnabled()) {
            logger.info("Not including " + id + ", not enabled");
            return false;
        }
        return true;
    }

    void clean(final Id id) {
        clean(id, CleanFlag.CONTAINER_AND_IMAGE);
    }

    void clean(final Id id, final CleanFlag flag) {
        cleanContainer(id);
        if (flag == CleanFlag.CONTAINER_AND_IMAGE) {
            cleanImage(id);
        }
    }

    private void cleanImage(final Id id) {
        if (id == null) {
            throw new IllegalArgumentException("id is null");
        }
        String imageId = null;
        try {
            imageId = findImageId(id);
        } catch (NotFoundException e) {
            logger.warn("Image " + id + " not found");
        } catch (DockerException e) {
            throw new OrchestrationException(e);
        }
        if (imageId != null) {
            logger.info("Removing image " + imageId);
            try {
                docker.removeImageCmd(imageId)
                        .withForce()
                        .exec();
            } catch (DockerException e) {
                logger.warn(e.getMessage());
            }
        }
    }

    private void cleanContainer(final Id id) {
        if (id == null) {
            throw new IllegalArgumentException("id is null");
        }
        stop(id);
        logger.info("Cleaning container " + id);
        for (Container container : findAllContainers(id)) {
            try {
                removeContainer(container);
            } catch (DockerException e) {
                throw new OrchestrationException(e);
            }
        }
    }

    private List<Container> findRunningContainers(Id id) {
        return findContainers(id, false);
    }

    private List<Container> findAllContainers(Id id) {
        return findContainers(id, true);
    }

    private List<Container> findContainers(Id id, boolean allContainers) {
        final List<Container> matchingContainers = new ArrayList<>();
        for (Container container : docker.listContainersCmd().withShowAll(allContainers).exec()) {
            boolean imageNameMatches = container.getImage().equals(repo.imageName(id));
            String[] containerNames = container.getNames();
            if (containerNames == null) {
                // Every container should have a name, but this is not the case
                // on Circle CI. Containers with no name are broken residues of
                // building the image and therefore will be ignored.
                continue;
            }
            boolean containerNameMatches = asList(containerNames).contains(containerName(id));
            if (imageNameMatches || containerNameMatches) {
                matchingContainers.add(container);
            }
        }
        return matchingContainers;
    }

    private String containerName(Id id) {
        Conf conf = repo.conf(id);
        if (conf == null) {
            throw new OrchestrationException(String.format("Unable to retrieve container name for id %s", id));
        }
        ContainerConf container = conf.getContainer();
        return container.hasName() ? container.getName() : repo.defaultContainerName(id);
    }

    void build(final Id id) {
        if (id == null) {
            throw new IllegalArgumentException("id is null");
        }
        try {
            build(prepare(id), id);
        } catch (IOException e) {
            throw new OrchestrationException(e);
        }

    }

    private void validate(final Id id) {
        if (id == null) {
            throw new IllegalArgumentException("id is null");
        }
        try {
            dockerfileValidator.validate(repo.src(id));
        } catch (IOException e) {
            throw new OrchestrationException(e);
        }
    }

    private File prepare(Id id) throws IOException {
        if (id == null) {
            throw new IllegalArgumentException("id is null");
        }
        logger.info("Preparing " + id);
        return fileOrchestrator.prepare(id, repo.src(id), conf(id));
    }

    @SuppressWarnings(("DM_DEFAULT_ENCODING"))
    private void build(File dockerFolder, Id id) {
        try {

            String tag = repo.tag(id);
            logger.info("Building " + id + " (" + tag + ")");

            final boolean noCache = buildNoCache();
            logger.info(" - no cache: " + noCache);

            final boolean removeIntermediateImages = buildRemoveIntermediateImages();
            logger.info(" - remove intermediate images: " + removeIntermediateImages);

            final boolean quiet = buildQuiet();
            logger.info(" - quiet: " + quiet);

            final boolean pull = buildPull();
            logger.info(" - pull: " + pull);

            BuildImageCmd build = docker.buildImageCmd(dockerFolder)
                    .withNoCache(noCache)
                    .withRemove(removeIntermediateImages)
                    .withQuiet(quiet)
                    .withTag(tag)
                    .withPull(pull);

            final BuildImageResultCallback callback = new BuildImageResultCallback() {

                @Override
                public void onNext(final BuildResponseItem item) {
                    super.onNext(item);
                    logger.info(buildResponseItemToString(item).replaceAll("\\r?\\n$", ""));
                }
            };

            final String imageId = build.exec(callback).awaitImageId();

            for (String otherTag : repo.conf(id).getTags()) {
                int lastIndexOfColon = otherTag.lastIndexOf(':');
                if (lastIndexOfColon > -1) {
                    String repositoryName = otherTag.substring(0, lastIndexOfColon);
                    String tagName = otherTag.substring(lastIndexOfColon + 1);
                    docker.tagImageCmd(imageId, repositoryName, tagName).withForce().exec();
                }
            }
        } catch (DockerException e) {
            throw new OrchestrationException(e);
        }
    }

    private String findImageId(Id id) {
        String imageTag = repo.tag(id);
        logger.debug("Converting {} ({}) to image id.", id, imageTag);
        List<Image> images = docker.listImagesCmd().exec();
        for (Image i : images) {
            for (String tag : i.getRepoTags()) {
                if (tag.startsWith(imageTag)) {
                    logger.debug("Using {} ({}) for {}. It matches (enough) to {}.", new Object[]{
                            i.getId(),
                            tag,
                            id.toString(),
                            imageTag});
                    return i.getId();
                }
            }
        }
        logger.debug("could not find image ID for \"" + id + "\" (tag \"" + imageTag + "\")");
        return null;
    }

    private boolean buildQuiet() {
        return haveBuildFlag(BuildFlag.QUIET);
    }

    private boolean buildPull() {
        return haveBuildFlag(BuildFlag.PULL);
    }

    private boolean buildRemoveIntermediateImages() {
        return haveBuildFlag(BuildFlag.REMOVE_INTERMEDIATE_IMAGES);
    }

    private boolean buildNoCache() {
        return haveBuildFlag(BuildFlag.NO_CACHE);
    }

    private boolean haveBuildFlag(BuildFlag flag) {
        return buildFlags.contains(flag);
    }

    private void start(final Id id) {
        if (id == null) {
            throw new IllegalArgumentException("id is null");
        }

        logger.info("Starting " + id);

        try {
            if (!imageExists(id)) {
                logger.info("Image does not exist, so building it");
                build(id);
            }
        } catch (DockerException e) {
            throw new OrchestrationException(e);
        }

        try {
            Container existingContainer = findContainer(id);

            if (existingContainer == null) {
                logger.info("No existing container for id {} so creating and starting new one", id);
                startContainer(createNewContainer(id));

            } else if (!isImageIdFromContainerMatchingProvidedImageId(existingContainer.getId(), id)) {
                logger.info("Image IDs do not match, removing container and creating new one from image");
                removeContainer(existingContainer);
                startContainer(createNewContainer(id));

            } else if (isRunning(id)) {
                logger.info("Container already running");

            } else {
                logger.info("Starting existing container " + existingContainer.getId());
                startContainer(existingContainer.getId());
            }

            Conf conf = conf(id);

            for (Plugin plugin : plugins) {
                plugin.started(id, conf);
            }

            sleep(id);

            healthCheck(id);

        } catch (Exception e) {
            logger.error("Error starting container with id " + id + ": " + e.getMessage());
            throw new OrchestrationException(e);
        }

        final Container container = findContainer(id);
        if (container == null) {
            throw new OrchestrationException("Could not find container with id " + id);
        }

        final Tail tail = tailFactory.newTail(docker, container, logger);
        tail.start();
    }

    private Container findContainer(Id id) {
        final List<Container> containerIds = findAllContainers(id);
        return containerIds.isEmpty() ? null : containerIds.get(0);
    }

    private boolean imageExists(Id id) throws DockerException {
        return findImageId(id) != null;
    }

    private void removeContainer(Container existingContainer) {
        logger.info("Removing container " + existingContainer.getId());
        try {
            docker.removeContainerCmd(existingContainer.getId()).withForce().withRemoveVolumes(true).exec();
        } catch (InternalServerErrorException e) {
            if (permissionErrorTolerant && isPermissionError(e)) {
                logger.warn(String.format("ignoring %s when removing container as we are configured to be permission error tolerant", e));
            } else {
                throw e;
            }
        }
    }

    private void sleep(Id id) {
        try {
            int sleep = conf(id).getSleep();
            if (sleep == 0) {
                return;
            }
            logger.info(String.format("Sleeping for %dms", sleep));
            Thread.sleep(sleep);
        } catch (InterruptedException e) {
            throw new OrchestrationException(e);
        }
    }

    private void waitForLogPatterns(final Id id, final List<LogPattern> logPatterns) {
        if (logPatterns == null || logPatterns.isEmpty()) {
            return;
        }

        final Container container;

        try {
            container = findContainer(id);
        } catch (DockerException e) {
            throw new OrchestrationException(e);
        }

        if (container == null) {
            logger.warn(String.format("Can not find container %s, not waiting", id));
            return;
        }

        final List<LogPattern> pendingPatterns = sortedLogPatterns(logPatterns);

        logger.info("Waiting for {} to appear in output", logPatternsToString(pendingPatterns));

        final LogContainerCmd logContainerCmd = docker.logContainerCmd(container.getId())
                .withStdErr()
                .withStdOut()
                .withTailAll()
                .withFollowStream();

        final PatternMatchingStartupResultCallback callback = new PatternMatchingStartupResultCallback(logger, pendingPatterns, id);

        int timeoutMax = 0;
        for (final LogPattern pattern : pendingPatterns) {
            timeoutMax = Math.max(timeoutMax, pattern.getTimeout());
        }

        try {
            logContainerCmd.exec(callback).awaitCompletion(timeoutMax, TimeUnit.MILLISECONDS);
            if (!callback.isComplete()) {
                callback.cancel();
                throw new OrchestrationException(String.format("timeout after %d while waiting for log-patterns in %s's log", timeoutMax, id));
            }
        } catch (InterruptedException e) {
            throw new OrchestrationException(String.format("Interrupt while waiting for log-patterns in %s's log", timeoutMax, id));
        }

        if (!callback.isComplete()) {
            throw new OrchestrationException(String.format("%s's log ended before %s appeared in output", id, logPatternsToString(pendingPatterns)));
        }
    }

    private boolean isImageIdFromContainerMatchingProvidedImageId(String containerId, final Id id) {
        try {
            String containerImageId = lookupImageIdFromContainer(containerId);
            String imageId = findImageId(id);
            return containerImageId.equals(imageId);
        } catch (DockerException e) {
            logger.error("Unable to find image with id " + id, e);
            throw new OrchestrationException(e);
        }

    }

    private String lookupImageIdFromContainer(String containerId) {
        try {
            InspectContainerResponse containerInspectResponse = docker.inspectContainerCmd(containerId).exec();
            return containerInspectResponse.getImageId();
        } catch (DockerException e) {
            logger.error("Unable to inspect container " + containerId, e);
            throw new OrchestrationException(e);
        }
    }

    private void startContainer(String idOfContainerToStart) {
        try {
            docker.startContainerCmd(idOfContainerToStart).exec();
        } catch (Exception e) {
            logger.error("Unable to start container " + idOfContainerToStart, e);
            throw new OrchestrationException(e);
        }
    }

    private Conf conf(Id id) {
        return repo.conf(id);
    }

    private String createNewContainer(Id id) throws DockerException {

        CreateContainerCmd cmd = docker.createContainerCmd(findImageId(id));

        Conf conf = conf(id);

        cmd.withPublishAllPorts(true);
        cmd.withPrivileged(conf.isPrivileged());
        cmd.withNetworkMode(conf.getNetworkMode());

        Link[] links = links(id);

        logger.info(" - links " + conf.getLinks());
        cmd.withLinks(links);

        logger.info(" - volumesFrom " + conf.getVolumesFrom());
        VolumesFrom[] volumesFrom = volumesFrom(id);
        cmd.withVolumesFrom(volumesFrom);

        List<PortBinding> portBindings = new ArrayList<>();
        for (String e : conf.getPorts()) {

            final String[] split = e.split(" ");

            assert split.length == 1 || split.length == 2;

            final int hostPort = Integer.parseInt(split[0]);
            final int containerPort = split.length == 2 ? Integer.parseInt(split[1]) : hostPort;

            logger.info(" - port " + hostPort + "->" + containerPort);
            portBindings.add(new PortBinding(new Ports.Binding(hostPort),
                    new ExposedPort(containerPort, InternetProtocol.TCP)));
        }
        cmd.withPortBindings(portBindings.toArray(new PortBinding[portBindings.size()]));

        logger.info(" - volumes " + conf.getVolumes());

        final List<Volume> volumes = new ArrayList<>();
        final List<Bind> binds = new ArrayList<>();
        for (Entry<String, String> entry : conf.getVolumes().entrySet()) {
            String volumePath = entry.getKey();
            Volume volume = new Volume(volumePath);
            
            String hostPath = entry.getValue();
            if (hostPath!=null && !hostPath.trim().equals("")){
                logger.info(" - volumes " + volumePath + " <- " + hostPath);
                binds.add(new Bind(hostPath, volume));
            } else {
            	volumes.add(volume);
            }
        }
        cmd.withVolumes(volumes.toArray(new Volume[volumes.size()]));
        cmd.withBinds(binds.toArray(new Bind[binds.size()]));

        cmd.withName(repo.containerName(id));
        logger.info(" - env " + conf.getEnv());
        cmd.withEnv(asEnvList(conf.getEnv()));

        if (!conf.getExtraHosts().isEmpty()) {
            List<String> extraHosts = conf.getExtraHosts();
            cmd.withExtraHosts(extraHosts.toArray(new String[extraHosts.size()]));
            logger.info(" - extra hosts " + conf.getExtraHosts());
        }

        final CreateContainerResponse createResponse = cmd.exec();

        final String[] warnings = createResponse.getWarnings();
        if (warnings != null) {
            for (final String warning : warnings) {
                logger.warn("Warning during container creation: " + warning);
            }
        }

        final String returnId = createResponse.getId();

        logger.info("Created new container " + returnId + " for " + id);

        return returnId;
    }

    /**
     * Converts String to String map to list of
     * key=value strings.
     */
    private String[] asEnvList(Map<String, String> env) {
        ArrayList<String> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            list.add(entry.getKey() + "=" + entry.getValue());
        }
        return list.toArray(new String[list.size()]);
    }

    private boolean isRunning(Id id) {
        if (id == null) {
            throw new IllegalArgumentException("id is null");
        }
        boolean running = false;
        final Container candidate = findContainer(id);
        for (Container container : docker.listContainersCmd().withShowAll(false).exec()) {
            running |= candidate != null && candidate.getId().equals(container.getId());
        }
        return running;
    }

    private void healthCheck(Id id) {
        final HealthChecks healthChecks = conf(id).getHealthChecks();
        waitForLogPatterns(id, healthChecks.getLogPatterns());
        waitForPings(id, healthChecks.getPings());
    }

    private void waitForPings(Id id, List<Ping> pings) {
        for (Ping ping : pings) {
            waitForPing(id, ping);
        }
    }

    private void waitForPing(Id id, Ping ping) {
        URI uri;
        if (ping.getUrl().toString().contains(CONTAINER_IP_PATTERN)) {
            try {
                uri = new URI(ping.getUrl().toString().replace(CONTAINER_IP_PATTERN, getIPAddress(id)));
            } catch (URISyntaxException e) {
                throw new OrchestrationException("Bad health check URI syntax: " + e.getMessage() + ", input: " + e.getInput() + ", index:" + e.getIndex());
            }
        } else {
            uri = ping.getUrl();
        }
        logger.info(String.format("Pinging %s for pattern \"%s\"", uri, ping.getPattern()));

        if (!Pinger.ping(uri, ping.getPattern(), ping.getTimeout(), ping.isSslVerify())) {
            throw new OrchestrationException("timeout waiting for " + uri + " for " + ping.getTimeout() + " with pattern " + ping.getPattern());
        }
    }

    private Link[] links(Id id) {
        final List<com.alexecollins.docker.orchestration.model.Link> links = conf(id).getLinks();
        final Link[] out = new Link[links.size()];
        final Set<String> seenAliases = Sets.newHashSet();
        for (int i = 0; i < links.size(); i++) {
            com.alexecollins.docker.orchestration.model.Link link = links.get(i);
            Container container = findContainer(link.getId());
            if (container == null) {
                throw new OrchestrationException(String.format("Could not find container for link %s", link.getId()));
            }
            final String name = com.alexecollins.docker.orchestration.util.Links.name(container.getNames());
            final String alias = link.getAlias();
            if (seenAliases.contains(alias)) {
                throw new OrchestrationException(String.format("Alias %s already used for a link with container %s", alias, id));
            }
            seenAliases.add(alias);
            out[i] = new Link(name, alias);
        }
        return out;
    }

    private VolumesFrom[] volumesFrom(Id id) {
        final List<com.alexecollins.docker.orchestration.model.VolumeFrom> volumes = conf(id).getVolumesFrom();
        final VolumesFrom[] out = new VolumesFrom[volumes.size()];
        for (int i = 0; i < volumes.size(); i++) {
            com.alexecollins.docker.orchestration.model.VolumeFrom volume = volumes.get(i);
            final Container container = findContainer(volume.getId());
            if (container == null) {
                throw new OrchestrationException(String.format(
                        "Can not use volume %s, unable to find corresponding container.", volume.getId()));
            }
            final AccessMode accessMode = AccessMode.fromBoolean(volume.isReadWrite());
            out[i] = new VolumesFrom(volume.getId().toString(), accessMode);
        }
        return out;
    }

    private void stop(final Id id) {
        if (id == null) {
            throw new IllegalArgumentException("id is null");
        }

        logger.info("Stopping " + id);

        for (Container container : findRunningContainers(id)) {
            logger.info("Stopping container " + Arrays.toString(container.getNames()));
            try {
                docker.stopContainerCmd(container.getId()).withTimeout(1).exec();
            } catch (DockerException e) {
                throw new OrchestrationException(e);
            }
        }
        for (Plugin plugin : plugins) {
            plugin.stopped(id, conf(id));
        }
    }

    private void copy(final Id id, final String resource, final String hostpath) {
        if (id == null) {
            throw new IllegalArgumentException("id is null");
        }

        logger.info("Copying " + resource + " from " + id + " to " + hostpath);
        if (findContainer(id) == null) {
            createNewContainer(id);
        }

        for (Container container : findAllContainers(id)) {
            try {
                // The copy command returns a "tar stream". Seems a less than friendly API, but ho-hum.
                InputStream is = docker.copyFileFromContainerCmd(container.getId(), resource).withHostPath(hostpath).exec();
                final TarArchiveInputStream tais = (TarArchiveInputStream) new ArchiveStreamFactory().createArchiveInputStream("tar", is);
                File targetPath = new File(hostpath);  // hostpath could be a file or a directory

                TarArchiveEntry tarEntry = tais.getNextTarEntry();
                while (tarEntry != null) {
                    File destPath = targetPath;
                    if (targetPath.isDirectory()) {
                        destPath = new File(targetPath, tarEntry.getName());
                    }
                    if (tarEntry.isDirectory() && destPath.isDirectory()) {
                        destPath.mkdirs();
                    } else {
                        Files.copy(tais, destPath.toPath());
                    }
                    tarEntry = tais.getNextTarEntry();
                }
                tais.close();
                is.close();
             } catch (DockerException | IOException | ArchiveException e) {
                throw new OrchestrationException(e);
            }
        }
    }

    public void build() {
        for (Id id : ids()) {
            if (!inclusive(id)) {
                continue;
            }
            build(id);
        }
    }

    public void validate() {
        Exception innerException = null;
        for (Id id : ids()) {
            if (!inclusive(id)) {
                continue;
            }
            try {
                validate(id);
            } catch (Exception e) {
                innerException = e;
            }
        }
        if (innerException != null)
            throw new OrchestrationException(innerException);
    }

    public void start() {
        for (Id id : ids()) {
            if (!inclusive(id)) {
                continue;
            }
            start(id);
        }
    }

    public void copy(String resource, String hostpath) {
        for (Id id : ids()) {
            if (!inclusive(id)) {
                continue;
            }
            copy(id, resource, hostpath);
        }
    }

    public String getIPAddress(Id id) {
        Container container = findContainer(id);
        if (container != null && repo.conf(id).isExposeContainerIp()) {
            InspectContainerResponse containerInspectResponse = docker.inspectContainerCmd(container.getId()).exec();
            return containerInspectResponse.getNetworkSettings().getIpAddress();
        } else {
            throw new IllegalArgumentException(id + " container IP address is not exposed");
        }
    }

    public Map<String, String> getIPAddresses() {
        Map<String, String> idToIpAddressMap = new HashMap<>();
        for (Id id : ids()) {
            Conf conf = repo.conf(id);
            if (inclusive(id) && conf.isExposeContainerIp()) {
                idToIpAddressMap.put(id.toString(), getIPAddress(id));
            }
        }
        return idToIpAddressMap;
    }

    public void stop() {
        for (Id id : repo.ids(true)) {
            if (!inclusive(id)) {
                continue;
            }
            stop(id);
        }
    }

    public List<Id> ids() {
        return repo.ids(false);
    }

    public void push() {
        for (Id id : ids()) {
            if (!inclusive(id)) {
                continue;
            }
            push(id);
        }
    }

    private void push(Id id) {
        for (final Identifier identifier : identifiers(id)) {
            try {
                final PushImageCmd pushImageCmd = docker.pushImageCmd(identifier.repository.name);

                if (identifier.tag.isPresent()) {
                    pushImageCmd.withTag(identifier.tag.get());
                }

                logger.info("Pushing " + id + " (" + asString(identifier) + ")");

                final PushImageResultCallback callback = new PushImageResultCallback() {

                    public void onNext(final PushResponseItem item) {
                        super.onNext(item);
                        logger.info(buildResponseItemToString(item).replaceAll("\\r?\\n$", ""));
                    }

                };

                pushImageCmd.exec(callback).awaitSuccess();

            } catch (DockerException | DockerClientException e) {
                throw new OrchestrationException(e);
            }
        }
    }

    private String asString(final Identifier identifier) {
        if (identifier == null) {
            return "";
        }

        final Optional<String> tag = identifier.tag;

        return identifier.repository.getPath() + (tag.isPresent() ? ":" + tag.get() : "");
    }

    private Iterable<Identifier> identifiers(Id id) {
        return FluentIterable.from(repo.tags(id))
                .transform(new Function<String, Identifier>() {
                    @Override
                    public Identifier apply(final String tag) {
                        final String repository = tag.replaceFirst(":[^:]*$", "");

                        if (tag.equals(repository)) {
                            return new Identifier(new Repository(repository), null);
                        } else {
                            return new Identifier(new Repository(repository),tag.substring(repository.length() + 1));
                        }
                    }
                }).toSortedSet(Ordering.usingToString());
    }

    public boolean isRunning() {
        for (Id id : ids()) {
            if (!isRunning(id)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    <P extends Plugin> P getPlugin(Class<P> pluginClass) {
        for (Plugin plugin : plugins) {
            if (plugin.getClass().equals(pluginClass)) {
                return (P) plugin;
            }
        }
        throw new NoSuchElementException("plugin " + pluginClass + " is not loaded");
    }

    /**
     * Save the images to files.
     * <p/>
     * Very much like "docker save ..."
     *
     * @param destDir Where to save them.
     * @param gzip    Gzip the output.
     *                @return What's been saved where.
     */
    public Map<Id, File> save(File destDir, boolean gzip) {

        Map<Id, File> saved = new HashMap<>();
        for (Id id : ids()) {
            if (!inclusive(id)) {
                continue;
            }

            File outputFile = new File(destDir, id + ".tar" + (gzip ? ".gz" : ""));

            logger.info("Saving {} as {}", id, outputFile);

            if (!imageExists(id)) {
                throw new OrchestrationException("image for " + id + " does not exist");
            }

            Conf conf = conf(id);
            String name = conf.hasTag() ? conf.getTag() : findImageId(id);

            if (!conf.hasTag()) {
                logger.warn("Image does NOT have tag. Saving using image ID. Export will be missing 'repositories' data.");
            }

            logger.info("Saving image {}", name);

            try (InputStream in = docker.saveImageCmd(name).exec();
                 OutputStream out = gzip ? new GZIPOutputStream(new FileOutputStream(outputFile)) : new FileOutputStream(outputFile)) {
                IOUtils.copy(in, out);
            } catch (IOException e) {
                throw new OrchestrationException(e);
            }

            saved.put(id, outputFile);
        }
        return saved;
    }

    public Map<Id, File> saveLogs(File destDir) {
        final Map<Id, File> saved = new HashMap<>();

        for (Id id : repo.ids(true)) {
            if (!inclusive(id)) {
                continue;
            }

            final Container container = findContainer(id);

            if (container != null) {
                final LogContainerCmd logContainerCmd = docker.logContainerCmd(container.getId())
                        .withStdErr()
                        .withStdOut()
                        .withTailAll();

                final CollectingLogContainerResultCallback callback = new CollectingLogContainerResultCallback();

                try {
                    logContainerCmd.exec(callback).awaitCompletion();
                } catch (InterruptedException e) {
                    throw new OrchestrationException("Failed to get output of container " + container.getId(), e);
                }

                final String logOutput = callback.getLogOutput();

                final File outputFile = new File(destDir, id + ".log");

                try {
                    try (final FileOutputStream fos = new FileOutputStream(outputFile)) {
                        IOUtils.write(logOutput, fos, Charsets.UTF_8);
                        logger.info("Wrote output of " + container.getId() + " to " + outputFile.getAbsolutePath());
                    }

                    saved.put(id, outputFile);
                } catch (IOException e) {
                    throw new OrchestrationException("Failed to write " + outputFile.getAbsolutePath(), e);
                }


            } else {
                logger.warn("Could not find container for image " + id);
            }
        }

        return saved;
    }

}
