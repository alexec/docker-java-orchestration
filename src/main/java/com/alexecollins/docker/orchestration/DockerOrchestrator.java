package com.alexecollins.docker.orchestration;


import com.alexecollins.docker.orchestration.model.*;
import com.alexecollins.docker.orchestration.util.Pinger;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.NotFoundException;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copyLarge;

/**
 * Orchestrates multiple Docker containers based on
 */
public class DockerOrchestrator {
	public static final String DEFAULT_HOST = "http://127.0.0.1:2375";
	public static final FileFilter DEFAULT_FILTER = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return false;
		}
	};
	public static final Properties DEFAULT_PROPERTIES = new Properties();

	private static final Logger LOGGER = LoggerFactory.getLogger(DockerOrchestrator.class);
	private static final int snooze = 0;

	private final DockerClient docker;
	private final Repo repo;

    private final FileOrchestrator fileOrchestrator;
	private final Set<BuildFlag> buildFlags;

    public DockerOrchestrator(DockerClient docker, File src, File workDir, File rootDir, String prefix, FileFilter filter, Properties properties) {
        this(docker, new Repo(docker, prefix, src, properties), new FileOrchestrator(workDir, rootDir, filter, properties), EnumSet.noneOf(BuildFlag.class));
    }

	public DockerOrchestrator(DockerClient docker, File src, File workDir, File rootDir, String prefix, FileFilter filter, Properties properties, Set<BuildFlag> buildFlags) {
        this(docker, new Repo(docker, prefix, src, properties), new FileOrchestrator(workDir, rootDir, filter, properties), buildFlags);
	}

	public DockerOrchestrator(DockerClient docker, Repo repo, FileOrchestrator fileOrchestrator) {
		this(docker,repo, fileOrchestrator, EnumSet.noneOf(BuildFlag.class));
	}

    public DockerOrchestrator(DockerClient docker, Repo repo, FileOrchestrator fileOrchestrator, Set<BuildFlag> buildFlags) {
	    if (docker == null) {
            throw new IllegalArgumentException("docker is null");
        }
        if (repo == null) {
            throw new IllegalArgumentException("repo is null");
        }
	    if (buildFlags == null) {throw new IllegalArgumentException("buildFlags is null");}


        this.docker = docker;
        this.repo = repo;
        this.fileOrchestrator = fileOrchestrator;

	    this.buildFlags = buildFlags;
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
		LOGGER.info("Clean " + id);
		for (Container container : repo.findContainers(id, true)) {
			LOGGER.info("Removing container " + container.getId());
			try {
				docker.removeContainerCmd(container.getId()).withForce().exec();
			} catch (DockerException e) {
				throw new OrchestrationException(e);
			}
		}
		String imageId = null;
		try {
			imageId = repo.getImageId(id);
		} catch (NotFoundException e) {
			LOGGER.warn("Image " + id + " not found");
		} catch (DockerException e) {
			throw new OrchestrationException(e);
		}
		if (imageId != null) {
            LOGGER.info("Removing image " + imageId);
            try {
				docker.removeImageCmd(imageId).exec();
			} catch (DockerException e) {
				LOGGER.warn(e.getMessage());
			}
		}
		snooze();
	}

	void build(final Id id) {
		if (id == null) {
			throw new IllegalArgumentException("id is null");
		}
		LOGGER.info("Package " + id);
		try {
			build(prepare(id), id);
		} catch (IOException e) {
			throw new OrchestrationException(e);
		}

		snooze();
	}

	private void snooze() {
        if (snooze == 0) {
            return;
        }
        LOGGER.info("Snoozing for " + snooze + "ms");
		try {
			Thread.sleep(snooze);
		} catch (InterruptedException e) {
			throw new OrchestrationException(e);
		}
	}

    private File prepare(Id id) throws IOException {
        if (id == null) {
			throw new IllegalArgumentException("id is null");
		}
        return fileOrchestrator.prepare(id, repo.src(id), repo.conf(id));
    }




	@SuppressWarnings(("DM_DEFAULT_ENCODING"))
	private void build(File dockerFolder, Id id) {

		InputStream in;
		try {
            BuildImageCmd build = docker.buildImageCmd(dockerFolder);
            for(BuildFlag f : buildFlags){
                switch (f){
                    case NO_CACHE: build.withNoCache();break;
                    case REMOVE_INTERMEDIATE_IMAGES: build.withRemove(true);break;
                }
            }
            build.withTag(repo.imageName(id));
            in = build.exec();
		} catch (DockerException e) {
			throw new OrchestrationException(e);
		}

		final StringWriter out = new StringWriter();
		try {
			copyLarge(new InputStreamReader(in, Charset.defaultCharset()), out);
		} catch (IOException e) {
			throw new OrchestrationException(e);
		} finally {
			closeQuietly(in);
		}

		String log = out.toString();
		if (!log.contains("Successfully built")) {
			throw new IllegalStateException("failed to build, log missing lines in" + log);
		}

		snooze();
	}


    private void start(final Id id) {
        if (id == null) {
            throw new IllegalArgumentException("id is null");
        }

        try {
            Container existingContainer = repo.findContainer(id);

            if (existingContainer == null) {
                LOGGER.info("No existing container so creating and starting new one");
                String containerId = createNewContainer(id);
                startContainer(containerId, id);

            } else if (!isImageIdFromContainerMatchingProvidedImageId(existingContainer.getId(), id)) {
                LOGGER.info("Image IDs do not match, removing container and creating new one from image");
                docker.removeContainerCmd(existingContainer.getId()).exec();
                startContainer(createNewContainer(id), id);

            } else if(isRunning(id)) {
                LOGGER.info("Container " + id + " already running");

            } else {
                LOGGER.info("Starting existing container " + existingContainer.getId());
                startContainer(existingContainer.getId(), id);
            }

        } catch (DockerException e) {
            throw new OrchestrationException(e);
        }
	    snooze();
	    healthCheck(id);
    }

    private boolean isImageIdFromContainerMatchingProvidedImageId(String containerId, final Id id) {
        try {
            String containerImageId = lookupImageIdFromContainer(containerId);
            String imageId = repo.getImageId(id);
            return containerImageId.equals(imageId);
        } catch (DockerException e) {
            LOGGER.error("Unable to find image with id " + id, e);
            throw new OrchestrationException(e);
        }

    }

    private String lookupImageIdFromContainer(String containerId) {
        try {
            InspectContainerResponse containerInspectResponse = docker.inspectContainerCmd(containerId).exec();
            return containerInspectResponse.getImageId();
        } catch (DockerException e) {
            LOGGER.error("Unable to inspect container " + containerId, e);
            throw new OrchestrationException(e);
        }
    }

    private void startContainer(String idOfContainerToStart, final Id id) {
        try {
            LOGGER.info("Starting " + id);
            StartContainerCmd start = docker.startContainerCmd(idOfContainerToStart);

            newHostConfig(id,start);
            start.exec();
        } catch (DockerException e) {
            LOGGER.error("Unable to start container " + idOfContainerToStart, e);
            throw new OrchestrationException(e);
        }
    }


    private String createNewContainer(Id id) throws DockerException {
        LOGGER.info("Creating " + id);
        Conf conf = repo.conf(id);
        CreateContainerCmd createCmd = docker.createContainerCmd(repo.getImageId(id));
        createCmd.withName(repo.containerName(id));
        LOGGER.info(" - env " + conf.getEnv());
        createCmd.withEnv(asEnvList(conf.getEnv()));
        CreateContainerResponse response = createCmd.exec();
		snooze();
        return response.getId();
	}

    /**
     * Converts String to String map to list of
     * key=value strings.
     * @param env
     * @return
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
		final HealthChecks healthChecks = repo.conf(id).getHealthChecks();
		for (Ping ping : healthChecks.getPings()) {
			LOGGER.info("Pinging " + ping.getUrl());
			if (!Pinger.ping(ping.getUrl(), ping.getTimeout())) {
				throw new OrchestrationException("timeout waiting for " + ping.getUrl() + " for " + ping.getTimeout());
			}
		}
	}

	private void newHostConfig(Id id, StartContainerCmd config) {
		config.withPublishAllPorts(true);

        Link[] links = links(id);
        LOGGER.info(" - links " + repo.conf(id).getLinks());
        config.withLinks(links);

		final Ports portBindings = new Ports();
		for (String e : repo.conf(id).getPorts()) {

			final String[] split = e.split(" ");

			assert split.length == 1 || split.length == 2;

			final int a = Integer.parseInt(split[0]);
			final int b = split.length == 2 ? Integer.parseInt(split[1]) : a;

			LOGGER.info(" - port " + e);
            portBindings.bind(new ExposedPort(a, InternetProtocol.TCP), new Ports.Binding(b));
        }
        config.withPortBindings(portBindings);

        LOGGER.info(" - volumes " + repo.conf(id).getVolumes());

        final List<Bind> binds = new ArrayList<Bind>();
        for (Map.Entry<String,String> entry : repo.conf(id).getVolumes().entrySet()) {
            String volumePath = entry.getKey();
            String hostPath = entry.getValue();
            File file = new File(hostPath);
            String path = file.getAbsolutePath();
            LOGGER.info(" - volumes " + volumePath +" <- "+ path);
            binds.add(new Bind(path, new Volume(volumePath)));
        }

		config.withBinds(binds.toArray(new Bind[binds.size()]));
	}

	private Link[] links(Id id) {
		final List<Id> links = repo.conf(id).getLinks();
		final Link[] out = new Link[links.size()];
		for (int i = 0; i < links.size(); i++) {
			final String name = repo.findContainer(links.get(i)).getNames()[0];
			out[i] = new Link(name,name);
		}
		return out;
	}

	private void stop(final Id id) {
		if (id == null) {
			throw new IllegalArgumentException("id is null");
		}
		for (Container container : repo.findContainers(id, false)) {
			LOGGER.info("Stopping " + Arrays.toString(container.getNames()));
			try {
				docker.stopContainerCmd(container.getId()).withTimeout(1).exec();
			} catch (DockerException e) {
				throw new OrchestrationException(e);
			}
			snooze();
		}
	}

	public void build() {
		for (Id id : ids()) {
			build(id);
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
            docker.pushImageCmd(repo.imageName(id)).withAuthConfig(docker.authConfig()).exec();
        } catch (DockerException e) {
			throw new OrchestrationException(e);
		}
		snooze();
	}

	public boolean isRunning() {
		for (Id id : ids()) {
			if (!isRunning(id)) {
				return false;
			}
		}
		return true;
	}
}
