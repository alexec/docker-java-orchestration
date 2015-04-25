package com.alexecollins.docker.orchestration;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.FrameReader;
import org.slf4j.Logger;

import java.io.IOException;

class Tail extends Thread {
    private final FrameReader frameReader;
    private final Logger logger;
    private int numLines = 0;
    private int maxLines = Integer.MAX_VALUE;
    private volatile boolean cancelled;

    Tail(DockerClient docker, Container container, Logger logger) {
        this.logger = logger;
        LogContainerCmd logContainerCmd = docker
                .logContainerCmd(container.getId())
                .withStdErr()
                .withStdOut()
                .withFollowStream()
                .withTail(10);

        frameReader = new FrameReader(logContainerCmd.exec());
    }

    void setMaxLines(int maxLines) {
        this.maxLines = maxLines;
    }

    @Override
    public void run() {
        Frame l;
        try {
            while (numLines < maxLines && !cancelled && (l = frameReader.readFrame()) != null) {
                logger.info(l.toString());
                numLines++;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void cancel() throws InterruptedException {
        cancelled = true;
        join();
    }
}
