package com.alexecollins.docker.orchestration;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.FrameReader;
import org.slf4j.Logger;

import javax.ws.rs.ProcessingException;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

class Tail extends Thread implements AutoCloseable {
    private final Logger logger;
    private final InputStream inputStream;
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

        inputStream = logContainerCmd.exec();
    }

    void setMaxLines(int maxLines) {
        this.maxLines = maxLines;
    }

    @Override
    public void run() {
        Frame l;
        try (FrameReader frameReader = new FrameReader(inputStream)) {
            while (numLines < maxLines && !cancelled && (l = frameReader.readFrame()) != null) {
                logger.info(l.toString());
                numLines++;
            }
        } catch (IOException e) {
            // silently swallow this message
            if (!e.getMessage().equals("Stream closed")) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void close() throws IOException, InterruptedException {
        cancelled = true;
        try {
            inputStream.close();
        } catch (ProcessingException ignore) {
            // something strang happens here, but as we are done with the stream, silently swallow it
        }
    }
}
