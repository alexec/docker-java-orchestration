package com.alexecollins.docker.orchestration;


import com.alexecollins.docker.orchestration.model.Credentials;
import com.alexecollins.docker.orchestration.model.HealthChecks;
import com.alexecollins.docker.orchestration.model.Id;
import com.alexecollins.docker.orchestration.model.Ping;
import com.alexecollins.docker.orchestration.util.Pinger;
import com.kpelykh.docker.client.DockerClient;
import com.kpelykh.docker.client.DockerException;
import com.kpelykh.docker.client.model.*;
import com.sun.jersey.api.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copyLarge;

/**
 * Orchestrates multiple Docker containers based on
 */
public class DockerOrchestrator {
	public static final String DEFAULT_HOST = "http://127.0.0.1:4243";
	public static final FileFilter DEFAULT_FILTER = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return false;
		}
	};
	public static final Properties DEFAULT_PROPERTIES = new Properties();

	private static final Logger LOGGER = LoggerFactory.getLogger(DockerOrchestrator.class);
	private static final int snooze = 1000;

	private final DockerClient docker;
	private final Repo repo;

    private final FileOrchestrator fileOrchestrator;

    /**
     * @deprecated Does not support API version.
     */
    @Deprecated
	public DockerOrchestrator(File src, File workDir, File rootDir, String prefix, Credentials credentials) {
		this(defaultDockerClient(), src, workDir, rootDir, prefix, credentials, DEFAULT_FILTER, DEFAULT_PROPERTIES);
	}

    public DockerOrchestrator(DockerClient docker, File src, File workDir, File rootDir, String prefix, Credentials credentials, FileFilter filter, Properties properties) {
        this(docker, new Repo(docker, prefix, src), new FileOrchestrator(workDir, rootDir, filter, properties), credentials);
    }

	private static DockerClient defaultDockerClient() {
        try {
            return new DockerClient();
        } catch (DockerException e) {
            throw new OrchestrationException(e);
        }
    }

    public DockerOrchestrator(DockerClient docker,  Repo repo, FileOrchestrator fileOrchestrator, Credentials credentials) {
        if (docker == null) {
            throw new IllegalArgumentException("docker is null");
        }
        if (repo == null) {
            throw new IllegalArgumentException("repo is null");
        }


        this.docker = docker;
        this.repo = repo;
        this.fileOrchestrator = fileOrchestrator;

        if (credentials != null) {
            docker.setCredentials(credentials.username, credentials.password, credentials.email);
        }

    }

	public void clean() {
		for (Id id : repo.ids(true)) {
			stop(id);
			clean(id);
		}
	}

	private void clean(final Id id) {
		if (id == null) {
			throw new IllegalArgumentException("id is null");
		}
		stop(id);
		LOGGER.info("clean " + id);
		for (Container container : repo.findContainers(id, true)) {
			LOGGER.info("rm " + Arrays.toString(container.getNames()));
			try {
				docker.removeContainer(container.getId());
			} catch (DockerException e) {
				throw new OrchestrationException(e);
			}
		}
		final Image image;
		try {
			image = repo.findImage(id);
		} catch (DockerException e) {
			throw new OrchestrationException(e);

		}
		if (image != null) {
			LOGGER.info("rmi " + image.getId());
			try {
				docker.removeImage(image.getId());
			} catch (DockerException e) {
				LOGGER.warn(" - " + e.getMessage());
			}
		}
		snooze();
	}

	private void build(final Id id) {
		if (id == null) {
			throw new IllegalArgumentException("id is null");
		}
		LOGGER.info("package " + id);
		try {
			build(prepare(id), id);
		} catch (IOException e) {
			throw new OrchestrationException(e);
		}

		snooze();
	}

	private void snooze() {
		LOGGER.info("snoozing for " + snooze + "ms");
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

		final ClientResponse response;
		try {
			response = docker.build(dockerFolder, repo.imageName(id));
		} catch (DockerException e) {
			throw new OrchestrationException(e);
		}

		final StringWriter out = new StringWriter();
		try {
			copyLarge(new InputStreamReader(response.getEntityInputStream(), Charset.defaultCharset()), out);
		} catch (IOException e) {
			throw new OrchestrationException(e);
		} finally {
			closeQuietly(response.getEntityInputStream());
		}

		String log = out.toString();
		if (!log.contains("Successfully built")) {
			throw new IllegalStateException("failed to build, log missing lines in" + log);
		}

		// imageId
		// return substringBetween(log, "Successfully built ", "\\n\"}").trim();

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
                LOGGER.info("Image ids don-t match, removing container and creating new one from image");
                docker.removeContainer(existingContainer.getId());
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
            String imageId = repo.findImage(id).getId();
            return containerImageId.equals(imageId);
        } catch (DockerException e) {
            LOGGER.error("Unable to find image with id " + id, e);
            throw new OrchestrationException(e);
        }

    }

    private String lookupImageIdFromContainer(String containerId) {
        try {
            ContainerInspectResponse containerInspectResponse = docker.inspectContainer(containerId);
            return containerInspectResponse.getImage();
        } catch (DockerException e) {
            LOGGER.error("Unable to inspect container " + containerId, e);
            throw new OrchestrationException(e);
        }
    }

    private void startContainer(String idOfContainerToStart, final Id id) {
        try {
            LOGGER.info("starting " + id);
            docker.startContainer(idOfContainerToStart, newHostConfig(id));
        } catch (DockerException e) {
            LOGGER.error("Unable to start container " + idOfContainerToStart, e);
            throw new OrchestrationException(e);
        }
    }


    private String createNewContainer(Id id) throws DockerException {
        LOGGER.info("creating " + id);
        final ContainerConfig config = new ContainerConfig();
        config.setImage(repo.findImage(id).getId());

		String newContainerId = docker.createContainer(config, repo.containerName(id)).getId();
		snooze();
        return newContainerId;
	}



	private boolean isRunning(Id id) {
		if (id == null) {throw new IllegalArgumentException("id is null");}
		boolean running = false;
		for (Container container : docker.listContainers(false)) {
			running |= repo.findContainer(id).getId().equals(container.getId());
		}
		return running;
	}

	private void healthCheck(Id id) {
		final HealthChecks healthChecks = repo.conf(id).getHealthChecks();
		for (Ping ping : healthChecks.getPings()) {
			LOGGER.info("pinging " + ping.getUrl());
			if (!Pinger.ping(ping.getUrl(), ping.getTimeout())) {
				throw new OrchestrationException("timeout waiting for " + ping.getUrl() + " for " + ping.getTimeout());
			}
		}
	}

    private List<Id> volumesFrom(Id id) {
		final List<Id> ids = new ArrayList<Id>();
		for (Id from : repo.conf(id).getVolumesFrom()) {
			ids.add(new Id(repo.findContainer(from).getId()));
		}

		return ids;
	}

	private HostConfig newHostConfig(Id id) {
		final HostConfig config = new HostConfig();

		config.setPublishAllPorts(true);
		config.setLinks(links(id));

		LOGGER.info(" - links " + repo.conf(id).getLinks());

		final Ports portBindings = new Ports();
		for (String e : repo.conf(id).getPorts()) {

			final String[] split = e.split(" ");

			assert split.length == 1 || split.length == 2;

			final int a = Integer.parseInt(split[0]);
			final int b = split.length == 2 ? Integer.parseInt(split[1]) : a;

			LOGGER.info(" - port " + e);
			portBindings.addPort(new Ports.Port("tcp", String.valueOf(a), null, String.valueOf(b)));
		}

		config.setPortBindings(portBindings);

		return config;
	}

	private String[] links(Id id) {

		final List<Id> links = repo.conf(id).getLinks();
		final String[] out = new String[links.size()];
		for (int i = 0; i < links.size(); i++) {
			final String name = repo.findContainer(links.get(i)).getNames()[0];
			out[i] = name + ":" + name;
		}

		return out;
	}

	private void stop(final Id id) {
		if (id == null) {
			throw new IllegalArgumentException("id is null");
		}
		for (Container container : repo.findContainers(id, false)) {
			LOGGER.info("stopping " + Arrays.toString(container.getNames()));
			try {
				docker.stopContainer(container.getId(), 1);
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
				if (repo.findImage(id) == null) {
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
			docker.push(repo.imageName(id));
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
