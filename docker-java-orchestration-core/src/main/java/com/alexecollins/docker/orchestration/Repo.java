package com.alexecollins.docker.orchestration;

import com.alexecollins.docker.orchestration.model.Conf;
import com.alexecollins.docker.orchestration.model.ContainerConf;
import com.alexecollins.docker.orchestration.model.Id;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

@SuppressWarnings("CanBeFinal")
class Repo {

    private static final Logger LOG = LoggerFactory.getLogger(Repo.class);
    private static ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
            .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);
    private final String user;
    private final String project;
    private final File src;
    private final Map<Id, Conf> confs = new LinkedHashMap<>();

    /**
     * @param user Name of the repo use. Maybe null.
     */
    @SuppressWarnings("ConstantConditions")
    Repo(String user, String project, File src, Properties properties) {
        if (user == null) {
            throw new IllegalArgumentException("user is null");
        }
        if (project == null) {
            throw new IllegalArgumentException("project is null");
        }
        if (src == null) {
            throw new IllegalArgumentException("src is null");
        }
        if (!src.isDirectory()) {
            throw new IllegalArgumentException("src " + src + " does not exist or is directory");
        }
        if (properties == null) {
            throw new IllegalArgumentException("properties is null");
        }

        this.user = user;
        this.project = project;
        this.src = src;

        if (src.isDirectory()) {
            readDockerConf(src, properties);
            ensureEmptyFolderConfs(src);
            readChildConfs(src, properties);
        }
    }

    private static Conf readConfFile(File confFile, Properties properties) throws IOException {
        return confFile.length() > 0 ? MAPPER.readValue(Confs.replacingReader(confFile, properties), Conf.class) : new Conf();
    }

    private void readDockerConf(File src, Properties properties) {
        // prioritise the docker.yml, especially for ordering
        File dockerConf = new File(src, "docker.yml");
        if (dockerConf.exists()) {
            LOG.info("reading " + dockerConf);
            try {
                confs.putAll(Confs.read(dockerConf, properties));
            } catch (IOException e) {
                throw new OrchestrationException(e);
            }
        }
    }

    private void readChildConfs(File src, Properties properties) {
        for (Id id : confs.keySet()) {
            File confFile = new File(src, id + "/conf.yml");
            if (confFile.exists()) {
                LOG.info("reading " + confFile);
                try {
                    confs.put(id, readConfFile(confFile, properties));
                } catch (IOException e) {
                    throw new OrchestrationException(e);
                }
            }
        }
    }

    private void ensureEmptyFolderConfs(File src) {
        for (File file : src.listFiles((FileFilter) DirectoryFileFilter.INSTANCE)) {
            Id id = new Id(file.getName());
            if (!confs.containsKey(id)) {
                confs.put(id, new Conf());
            }
        }
    }

    public String tag(Id id) {
        Conf conf = conf(id);
        return
                conf.hasTag()
                        ? conf.getTag()
                        : imageName(id);
    }

    public List<String> tags(Id id) {
        Conf conf = conf(id);
        return conf.hasTag()
                ? conf.getTags()
                : Collections.singletonList(imageName(id));
    }

    String imageName(Id id) {
        return user + "/" + project + "_" + id;
    }

    String containerName(Id id) {
        ContainerConf container = confs.get(id).getContainer();
        return container.hasName() ? container.getName() : defaultContainerName(id);
    }

    String defaultContainerName(Id id) {
        return "/" + project + "_" + id;
    }

    private File src() {
        return src;
    }


    File src(Id id) {
        return new File(src(), id.toString());
    }

    List<Id> ids(boolean reverse) {

        final Map<Id, List<Id>> links = new LinkedHashMap<>();
        for (Id id : confs.keySet()) {
            links.put(id, com.alexecollins.docker.orchestration.util.Links.ids(confs.get(id).getLinks()));
        }

        final List<Id> out = sort(links);
        if (reverse) {
            Collections.reverse(out);
        }

        return out;
    }

    List<Id> sort(final Map<Id, List<Id>> links) {
        final List<Id> in = new LinkedList<>(links.keySet());
        final List<Id> out = new LinkedList<>();

        final List<String> linkErrors = Lists.newLinkedList();
        for (final Map.Entry<Id,List<Id>> entry : links.entrySet()) {
            final Set<Id> linkedImages = Sets.newHashSet(entry.getValue());
            final Sets.SetView<Id> difference = Sets.difference(linkedImages, links.keySet());
            if (!difference.isEmpty()) {
                linkErrors.add("Missing linked containers in " + entry.getKey() + ": " + difference);
            }
            final Set<String> seenIds = Sets.newHashSet();
            for (final Id id: linkedImages) {
                final String linkName = id.toString().replaceFirst("[^:]+:","");
                if (seenIds.contains(linkName)) {
                    linkErrors.add("Configuration for " + entry.getKey() + " contains " + linkName + " multiple times (" + id.toString() + ")");
                }
                seenIds.add(linkName);
            }
        }
        if (!linkErrors.isEmpty()) {
            throw new IllegalStateException(Joiner.on('\n').join(linkErrors));
        }


        while (!in.isEmpty()) {
            boolean hit = false;
            for (Iterator<Id> iterator = in.iterator(); iterator.hasNext(); ) {
                final Id id = iterator.next();
                if (out.containsAll(links.get(id))) {
                    out.add(id);
                    iterator.remove();
                    hit = true;
                }
            }
            if (!hit) {
                throw new IllegalStateException("dependency error (e.g. circular dependency) amongst " + in);
            }
        }

        return out;
    }

    Conf conf(Id id) {
        return confs.get(id);
    }
}
