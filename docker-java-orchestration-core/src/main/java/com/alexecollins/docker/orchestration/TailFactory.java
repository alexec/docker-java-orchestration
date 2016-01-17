package com.alexecollins.docker.orchestration;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;

interface TailFactory {
    TailFactory DEFAULT = new TailFactory() {
        @Override
        public Tail newTail(DockerClient docker, Container container, Logger logger) {
            Preconditions.checkNotNull(docker, "docker must not be null");
            Preconditions.checkNotNull(container, "container must not be null");
            Preconditions.checkNotNull(logger, "logger must not be null");

            return new Tail(docker, container, logger);
        }
    };

    Tail newTail(DockerClient docker, Container container, Logger logger);
}
