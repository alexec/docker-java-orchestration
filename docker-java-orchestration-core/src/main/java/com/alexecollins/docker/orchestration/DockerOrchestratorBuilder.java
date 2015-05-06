package com.alexecollins.docker.orchestration;

import com.alexecollins.docker.orchestration.model.BuildFlag;
import com.alexecollins.docker.orchestration.util.TextFileFilter;
import com.github.dockerjava.api.DockerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;

public class DockerOrchestratorBuilder {
    private final DockerfileValidator dockerfileValidator = new DockerfileValidator();
    private DockerClient docker;
    private File src;
    private File workDir;
    private File rootDir;
    private String user;
    private String project;
    private FileFilter filter = TextFileFilter.INSTANCE;
    private Properties properties = new Properties();
    private Set<BuildFlag> buildFlags = EnumSet.noneOf(BuildFlag.class);
    private Logger logger = LoggerFactory.getLogger(DockerOrchestrator.class);
    private DefinitionFilter definitionFilter = DefinitionFilter.ANY;
    private boolean permissionErrorTolerant;

    DockerOrchestratorBuilder() {
    }

    public DockerOrchestratorBuilder docker(DockerClient docker) {
        this.docker = docker;
        return this;
    }

    public DockerOrchestratorBuilder src(File src) {
        this.src = src;
        return this;
    }

    public DockerOrchestratorBuilder workDir(File workDir) {
        this.workDir = workDir;
        return this;
    }

    public DockerOrchestratorBuilder rootDir(File rootDir) {
        this.rootDir = rootDir;
        return this;
    }

    public DockerOrchestratorBuilder user(String user) {
        this.user = user;
        return this;
    }

    public DockerOrchestratorBuilder project(String project) {
        this.project = project;
        return this;
    }

    public DockerOrchestratorBuilder filter(FileFilter filter) {
        this.filter = filter;
        return this;
    }

    public DockerOrchestratorBuilder properties(Properties properties) {
        this.properties = properties;
        return this;
    }

    public DockerOrchestratorBuilder buildFlags(Set<BuildFlag> buildFlags) {
        this.buildFlags = buildFlags;
        return this;
    }

    public DockerOrchestratorBuilder definitionFilter(DefinitionFilter definitionFilter) {
        this.definitionFilter = definitionFilter;
        return this;
    }

    DockerOrchestratorBuilder logger(Logger logger) {
        this.logger = logger;
        return this;
    }

    public DockerOrchestratorBuilder permissionErrorTolerant(boolean permissionErrorTolerant) {
        this.permissionErrorTolerant = permissionErrorTolerant;
        return this;
    }

    public DockerOrchestrator build() {
        return new DockerOrchestrator(
                docker,
                new Repo(user, project, src, properties),
                new FileOrchestrator(workDir, rootDir, filter, properties),
                buildFlags,
                logger,
                dockerfileValidator,
                definitionFilter,
                permissionErrorTolerant);
    }
}