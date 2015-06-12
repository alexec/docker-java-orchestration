package com.alexecollins.docker.orchestration;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import org.slf4j.Logger;

interface TailFactory {
    TailFactory DEFAULT = new TailFactory() {
        @Override
        public Tail newTail(DockerClient docker, Container container, Logger logger) {
            return new Tail(docker, container, logger);
        }
    };

    Tail newTail(DockerClient docker, Container container, Logger logger);
}
