package com.alexecollins.docker.orchestration;


import com.alexecollins.docker.orchestration.model.*;
import com.alexecollins.docker.orchestration.plugin.api.Plugin;
import com.alexecollins.docker.orchestration.util.Pinger;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.NotFoundException;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.api.model.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

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
	private final Repo repo;

    private final FileOrchestrator fileOrchestrator;
	private final Set<BuildFlag> buildFlags;
    private final List<Plugin> plugins = new ArrayList<Plugin>();
    private final DockerfileValidator dockerfileValidator;

    /**
     * @deprecated Please use builder from now on.
     */
    @Deprecated
    public DockerOrchestrator(DockerClient docker, File src, File workDir, File rootDir, String user, String project, FileFilter filter, Properties properties) {
        this(docker, new Repo(docker, user, project, src, properties), new FileOrchestrator(workDir, rootDir, filter, properties), EnumSet.noneOf(BuildFlag.class), DEFAULT_LOGGER, new DockerfileValidator());
    }

    /**
     * @deprecated Please use builder from now on.
     */
    @Deprecated
    public DockerOrchestrator(DockerClient docker, File src, File workDir, File rootDir, String user, String project, FileFilter filter, Properties properties, Set<BuildFlag> buildFlags) {
        this(docker, new Repo(docker, user, project, src, properties), new FileOrchestrator(workDir, rootDir, filter, properties), buildFlags, DEFAULT_LOGGER, new DockerfileValidator());
    }

    DockerOrchestrator(DockerClient docker, Repo repo, FileOrchestrator fileOrchestrator, Set<BuildFlag> buildFlags, Logger logger, DockerfileValidator dockerfileValidator) {
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

        this.docker = docker;
        this.repo = repo;
        this.fileOrchestrator = fileOrchestrator;
	    this.buildFlags = buildFlags;
        this.logger = logger;
        this.dockerfileValidator = dockerfileValidator;

        for (Plugin plugin : ServiceLoader.load(Plugin.class)) {
            plugins.add(plugin);
            logger.info("Loaded " + plugin.getClass() + " plugin");
        }
    }

    public static DockerOrchestratorBuilder builder() {
        return new DockerOrchestratorBuilder();
    }

    public void clean() {
        for (Id id : repo.ids(true)) {
            stop(id);
            clean(id);
        }
	}

	void clean(final Id id) {
		if (id == null) {
			throw new IllegalArgumentException("id is null");
		}
		stop(id);
		logger.info("Clean " + id);
		for (Container container : repo.findContainers(id, true)) {
			logger.info("Removing container " + container.getId());
			try {
				docker.removeContainerCmd(container.getId()).withForce().exec();
			} catch (DockerException e) {
				throw new OrchestrationException(e);
			}
		}
		String imageId = null;
		try {
            imageId = repo.findImageId(id);
        } catch (NotFoundException e) {
			logger.warn("Image " + id + " not found");
		} catch (DockerException e) {
			throw new OrchestrationException(e);
		}
		if (imageId != null) {
            logger.info("Removing image " + imageId);
            try {
                docker.removeImageCmd(imageId).withForce().exec();
            } catch (DockerException e) {
				logger.warn(e.getMessage());
			}
		}
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
        logger.info("Prepare " + id);
        return fileOrchestrator.prepare(id, repo.src(id), conf(id));
    }

    @SuppressWarnings(("DM_DEFAULT_ENCODING"))
    private void build(File dockerFolder, Id id) {
        try {
            BuildImageCmd build = docker.buildImageCmd(dockerFolder);
            for(BuildFlag f : buildFlags){
                switch (f){
                    case NO_CACHE: build = build.withNoCache();break;
                    case REMOVE_INTERMEDIATE_IMAGES: build = build.withRemove(true);break;
                }
            }
            String tag = repo.tag(id);
            build = build.withTag(tag);
            logger.info("Build " + id + " (" + tag + ")");
            throwExceptionIfThereIsAnError(build.exec());

            for (String otherTag : repo.conf(id).getTags()) {
                int lastIndexOfColon = otherTag.lastIndexOf(':');
                if (lastIndexOfColon > -1) {
                    String repositoryName = otherTag.substring(0, lastIndexOfColon);
                    String tagName = otherTag.substring(lastIndexOfColon + 1);
                    docker.tagImageCmd(repo.findImageId(id), repositoryName, tagName).withForce().exec();
                }
            }
        } catch (DockerException e) {
            throw new OrchestrationException(e);
        } catch (IOException e) {
            throw new OrchestrationException(e);
        }

    }

    private void start(final Id id) {
        if (id == null) {
            throw new IllegalArgumentException("id is null");
        }

        try {
            Container existingContainer = repo.findContainer(id);

            if (existingContainer == null) {
                logger.info("No existing container so creating and starting new one");
                String containerId = createNewContainer(id);
                startContainer(containerId, id);

            } else if (!isImageIdFromContainerMatchingProvidedImageId(existingContainer.getId(), id)) {
                logger.info("Image IDs do not match, removing container and creating new one from image");
                docker.removeContainerCmd(existingContainer.getId()).exec();
                startContainer(createNewContainer(id), id);

            } else if(isRunning(id)) {
                logger.info("Container " + id + " already running");

            } else {
                logger.info("Starting existing container " + existingContainer.getId());
                startContainer(existingContainer.getId(), id);
            }

        } catch (DockerException e) {
            throw new OrchestrationException(e);
        }
        healthCheck(id);
    }

    private boolean isImageIdFromContainerMatchingProvidedImageId(String containerId, final Id id) {
        try {
            String containerImageId = lookupImageIdFromContainer(containerId);
            String imageId = repo.findImageId(id);
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

    private void startContainer(String idOfContainerToStart, final Id id) {
        try {
            logger.info("Starting " + id);
            StartContainerCmd start = docker.startContainerCmd(idOfContainerToStart);

            prepareHostConfig(id, start);
            start.exec();

            for (Plugin plugin : plugins) {
                plugin.started(id, conf(id));
            }

        } catch (DockerException e) {
            logger.error("Unable to start container " + idOfContainerToStart, e);
            throw new OrchestrationException(e);
        }
    }

    private Conf conf(Id id) {
        return repo.conf(id);
    }

    private String createNewContainer(Id id) throws DockerException {
        logger.info("Creating " + id);
        Conf conf = conf(id);
        CreateContainerCmd createCmd = docker.createContainerCmd(repo.findImageId(id));
        createCmd.withName(repo.containerName(id));
        logger.info(" - env " + conf.getEnv());
        createCmd.withEnv(asEnvList(conf.getEnv()));
        CreateContainerResponse response = createCmd.exec();
        return response.getId();
	}

    /**
     * Converts String to String map to list of
     * key=value strings.
     */
    private String[] asEnvList(Map<String, String> env) {
        ArrayList<String> list = new ArrayList<String>();
        for(Map.Entry<String,String> entry : env.entrySet()){
            list.add(entry.getKey()+"="+entry.getValue());
        }
        return list.toArray(new String[list.size()]);
    }

    private boolean isRunning(Id id) {
		if (id == null) {throw new IllegalArgumentException("id is null");}
		boolean running = false;
        final Container candidate = repo.findContainer(id);
		for (Container container : docker.listContainersCmd().withShowAll(false).exec()) {
			running |= candidate != null && candidate.getId().equals(container.getId());
		}
		return running;
	}

    private void healthCheck(Id id) {
        final HealthChecks healthChecks = conf(id).getHealthChecks();
        for (Ping ping : healthChecks.getPings()) {
            URI uri;
            if (ping.getUrl().toString().contains(CONTAINER_IP_PATTERN)) {
                try {
                    uri = new URI(ping.getUrl().toString().replace(CONTAINER_IP_PATTERN, getIPAddresses().get(id.toString())));
                } catch (URISyntaxException e) {
                    throw new OrchestrationException("Bad health check URI syntax: " + e.getMessage() + ", input: " + e.getInput() + ", index:" + e.getIndex());
                }
            } else {
                uri = ping.getUrl();
            }
            logger.info("Pinging " + uri);

            if (!Pinger.ping(uri, ping.getPattern(), ping.getTimeout())) {
                throw new OrchestrationException("timeout waiting for " + uri + " for " + ping.getTimeout() + " with pattern " + ping.getPattern());
            }
        }
    }

	private void prepareHostConfig(Id id, StartContainerCmd config) {
		config.withPublishAllPorts(true);

        Link[] links = links(id);
        logger.info(" - links " + conf(id).getLinks());
        config.withLinks(links);

		final Ports portBindings = new Ports();
		for (String e : conf(id).getPorts()) {

			final String[] split = e.split(" ");

			assert split.length == 1 || split.length == 2;

			final int a = Integer.parseInt(split[0]);
			final int b = split.length == 2 ? Integer.parseInt(split[1]) : a;

			logger.info(" - port " + e);
            portBindings.bind(new ExposedPort(a, InternetProtocol.TCP), new Ports.Binding(b));
        }
        config.withPortBindings(portBindings);

        logger.info(" - volumes " + conf(id).getVolumes());

        final List<Bind> binds = new ArrayList<Bind>();
        for (Map.Entry<String,String> entry : conf(id).getVolumes().entrySet()) {
            String volumePath = entry.getKey();
            String hostPath = entry.getValue();
            File file = new File(hostPath);
            String path = file.getAbsolutePath();
            logger.info(" - volumes " + volumePath + " <- " + path);
            binds.add(new Bind(path, new Volume(volumePath)));
        }

		config.withBinds(binds.toArray(new Bind[binds.size()]));
	}

	private Link[] links(Id id) {
        final List<com.alexecollins.docker.orchestration.model.Link> links = conf(id).getLinks();
        final Link[] out = new Link[links.size()];
		for (int i = 0; i < links.size(); i++) {
            com.alexecollins.docker.orchestration.model.Link link = links.get(i);
            final String name = com.alexecollins.docker.orchestration.util.Links.name(repo.findContainer(link.getId()).getNames());
            final String alias = link.getAlias();
            out[i] = new Link(name, alias);
        }
		return out;
	}

	private void stop(final Id id) {
		if (id == null) {
			throw new IllegalArgumentException("id is null");
		}
		for (Container container : repo.findContainers(id, false)) {
			logger.info("Stopping " + Arrays.toString(container.getNames()));
			try {
				docker.stopContainerCmd(container.getId()).withTimeout(1).exec();
			} catch (DockerException e) {
				throw new OrchestrationException(e);
			}
            for (Plugin plugin : plugins) {
                plugin.stopped(id, conf(id));
            }
        }
    }

	public void build() {
		for (Id id : ids()) {
			build(id);
		}
	}

    public void validate() {
        for (Id id : ids()) {
            validate(id);
        }
    }

	public void start() {
		for (Id id : ids()) {
			try {
				if (!repo.imageExists(id)) {
					build(id);
				}
			} catch (DockerException e) {
				throw new OrchestrationException(e);
			}
			start(id);
		}
	}

    public Map<String, String> getIPAddresses() {
        Map<String, String> idToIpAddressMap = new HashMap<String, String>();
        for (Id id : ids()) {
            Conf conf = repo.conf(id);
            if (conf.isExposeContainerIp()) {
                String containerName = repo.containerName(id);
                InspectContainerResponse containerInspectResponse = docker.inspectContainerCmd(containerName).exec();
                idToIpAddressMap.put(id.toString(), containerInspectResponse.getNetworkSettings().getIpAddress());
            }
        }
        return idToIpAddressMap;
    }

	public void stop() {
		for (Id id : repo.ids(true)) {
			stop(id);
		}
	}

	public List<Id> ids() {
		return repo.ids(false);
	}

	public void push() {
		for (Id id : ids()) {
			push(id);
		}
	}

	private void push(Id id) {
		try {
            PushImageCmd pushImageCmd = docker.pushImageCmd(repo(id)).withAuthConfig(docker.authConfig());
            logger.info("Push " + id + " (" + pushImageCmd.getName() + ")");
            InputStream inputStream = pushImageCmd.exec();
            throwExceptionIfThereIsAnError(inputStream);
        } catch (DockerException e) {
			throw new OrchestrationException(e);
		} catch (IOException e) {
			throw new OrchestrationException(e);
        }
    }

    private String repo(Id id) {
        return repo.tag(id).replaceFirst(":[^:]*$", "");
    }

    private void throwExceptionIfThereIsAnError(InputStream exec) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(exec));
        String l;
        while ((l = reader.readLine()) != null) {
            logger.info(l);
            if (l.startsWith("{\"errorDetail")) {
                throw new OrchestrationException(extractMessage(l));
            }
        }
    }

    private String extractMessage(String l) {
        return l;
        //return l.replaceFirst(".*\"message\":\"([^\"]*)\".*", "$1");
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
                return (P)plugin;
            }
        }
        throw new NoSuchElementException("plugin " + pluginClass + " is not loaded");
    }
}
