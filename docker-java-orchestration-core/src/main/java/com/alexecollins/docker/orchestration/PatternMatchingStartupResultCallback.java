package com.alexecollins.docker.orchestration;

import com.alexecollins.docker.orchestration.model.Id;
import com.alexecollins.docker.orchestration.model.LogPattern;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.google.common.collect.Lists;
import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author marcus
 * @since 1.0.0
 */
public class PatternMatchingStartupResultCallback extends LogContainerResultCallback {

    private final Logger logger;
    private final Id id;
    private final StopWatch watch;
    private final List<LogPattern> pendingPatterns;
    private final AtomicReference<State> state = new AtomicReference<>(State.INITIAL);

    public PatternMatchingStartupResultCallback(final Logger logger, final List<LogPattern> pendingPatterns, final Id id) {
        this.pendingPatterns = Collections.synchronizedList(Lists.newLinkedList(pendingPatterns));
        this.id = id;
        this.logger = logger;

        watch = new StopWatch();
        watch.start();
    }

    @Override
    public void onNext(final Frame item) {
        if (isCanceled()) {
            return;
        }

        final String line = new String(item.getPayload()).trim();
        for (Iterator<LogPattern> iterator = pendingPatterns.iterator(); iterator.hasNext(); ) {
            LogPattern logPattern = iterator.next();
            if (logPattern.getPattern().matcher(line).find()) {
                logger.info("Waited {} for \"{}\"", watch, logPattern.getPattern().toString());
                iterator.remove();
            }
        }
        if (pendingPatterns.isEmpty()) {
            watch.stop();
            onComplete();
            return;
        }
        for (LogPattern logPattern : pendingPatterns) {
            if (watch.getTime() >= logPattern.getTimeout()) {
                throw new OrchestrationException(String.format("timeout after %d while waiting for \"%s\" in %s's logs", logPattern.getTimeout(), logPattern.getPattern(), id));
            }
        }
    }

    @Override
    public void onComplete() {
        if (isCanceled() || isComplete()) {
            return;
        }

        state.compareAndSet(State.INITIAL, State.COMPLETE);

        if (!pendingPatterns.isEmpty()) {
            throw new OrchestrationException(String.format("%s's log ended before %s appeared in output", id, DockerOrchestrator.logPatternsToString(pendingPatterns)));
        }

        super.onComplete();
    }

    public boolean isComplete() {
        return state.get() == State.COMPLETE;
    }

    public void cancel() {
        state.compareAndSet(State.INITIAL, State.CANCELED);
    }

    public boolean isCanceled() {
        return state.get() == State.CANCELED;
    }

    private enum State {
        INITIAL, COMPLETE, CANCELED
    }

}
