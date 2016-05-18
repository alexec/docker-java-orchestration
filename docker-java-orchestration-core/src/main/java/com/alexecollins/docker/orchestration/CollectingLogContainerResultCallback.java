package com.alexecollins.docker.orchestration;

import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.LogContainerResultCallback;

/**
 * @author marcus
 * @since 1.0.0
 */
public class CollectingLogContainerResultCallback extends LogContainerResultCallback {


    private final StringBuffer logOutput = new StringBuffer();

    @Override
    public void onNext(final Frame item) {
        logOutput.append(item.toString()).append("\n");
    }

    public String getLogOutput() {
        return logOutput.toString();
    }

}
