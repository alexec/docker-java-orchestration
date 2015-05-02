package com.alexecollins.docker.orchestration;

import com.alexecollins.docker.orchestration.model.Conf;
import com.alexecollins.docker.orchestration.model.ContainerConf;
import com.alexecollins.docker.orchestration.model.Id;
import com.alexecollins.docker.orchestration.util.PropertiesTokenResolver;
import com.alexecollins.docker.orchestration.util.TokenReplacingReader;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.io.filefilter.DirectoryFileFilter;

import java.io.*;
import java.util.*;

@SuppressWarnings("CanBeFinal")
class Repo {

    private static ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
            .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);
    private final String user;
    private final String project;
    private final File src;
    private final Map<Id, Conf> confs = new HashMap<>();

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
            for (File file : src.listFiles((FileFilter) DirectoryFileFilter.INSTANCE)) {
                final File confFile = new File(file, "conf.yml");
                try {
                    confs.put(new Id(file.getName()), confFile.length() > 0 ? MAPPER.readValue(confReader(confFile, properties), Conf.class) : new Conf());
                } catch (IOException e) {
                    throw new OrchestrationException(e);
                }
            }
        }
    }

    private static Reader confReader(File confFile, Properties properties) throws FileNotFoundException {
        return new TokenReplacingReader(new FileReader(confFile), new PropertiesTokenResolver(properties));
    }

    public String tag(Id id) {
        Conf conf = conf(id);
        return
                conf.hasTag()
                        ? conf.getTag()
                        : imageName(id);
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

        final List<Id> in = new LinkedList<>(confs.keySet());

        final Map<Id, List<Id>> links = new HashMap<>();
        for (Id id : in) {
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
