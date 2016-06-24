package com.alexecollins.docker.orchestration;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;

class Tail implements ResultCallback<Frame> {
    private final Logger logger;
    private final LogContainerCmd logContainerCmd;
    private int numLines = 0;
    private int maxLines = Integer.MAX_VALUE;
    private volatile boolean cancelled;

    Tail(DockerClient docker, Container container, Logger logger) {
        Preconditions.checkNotNull(container,"Container must not be null");
        this.logger = logger;
        this.logContainerCmd = docker.logContainerCmd(container.getId())
                .withStdErr(true)
                .withStdOut(true)
                .withTailAll();
    }

    @Override
    public void onStart(final Closeable closeable) {

    }

    @Override
    public void onNext(final Frame object) {
        if (numLines < maxLines && !cancelled) {
            logger.info(object.toString());
            numLines++;
        }
    }

    @Override
    public void onError(final Throwable throwable) {
        cancelled = true;
        throw new RuntimeException(throwable);
    }

    @Override
    public void onComplete() {
    }

    @Override
    public void close() throws IOException {
        cancelled = true;
    }


    void setMaxLines(int maxLines) {
        this.maxLines = maxLines;
    }

    public void start() {
        logContainerCmd.exec(this);
    }
}
