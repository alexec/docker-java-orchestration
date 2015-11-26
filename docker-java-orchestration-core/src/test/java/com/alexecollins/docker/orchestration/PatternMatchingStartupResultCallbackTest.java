package com.alexecollins.docker.orchestration;

import com.alexecollins.docker.orchestration.model.Id;
import com.alexecollins.docker.orchestration.model.LogPattern;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import com.google.common.base.Charsets;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author marcus
 * @since 1.0.0
 */
public class PatternMatchingStartupResultCallbackTest {

    private static PatternMatchingStartupResultCallback makeWithPattern(final String pattern, final int timeout) {
        final LogPattern logPattern = new LogPattern(pattern);
        logPattern.setTimeout(timeout);
        return makeWithLogPatterns(Collections.singletonList(logPattern));
    }

    private static PatternMatchingStartupResultCallback makeWithPattern(final String pattern) {
        return makeWithPattern(pattern, Integer.MAX_VALUE);
    }

    private static PatternMatchingStartupResultCallback makeWithLogPatterns(final List<LogPattern> patterns) {
        final Logger logger = Mockito.mock(Logger.class);
        return new PatternMatchingStartupResultCallback(logger, patterns, new Id("test"));
    }

    private static PatternMatchingStartupResultCallback makeEmptyCallbackInstance() {
        return makeWithLogPatterns(Collections.<LogPattern>emptyList());
    }

    private static Frame frame(final String value) {
        return new Frame(StreamType.STDOUT, value.getBytes(Charsets.UTF_8));
    }

    @Test
    public void shouldBeCanceledAfterCancel() {
        final PatternMatchingStartupResultCallback callback = makeEmptyCallbackInstance();
        assertThat("Should not be canceled", callback.isCanceled(), is(false));
        assertThat("Should not be complete", callback.isComplete(), is(false));

        callback.cancel();

        assertThat("Should now be canceled", callback.isCanceled(), is(true));
        assertThat("Should not be complete", callback.isComplete(), is(false));
    }

    @Test
    public void shouldBeCompleteAfterCancel() {
        final PatternMatchingStartupResultCallback callback = makeEmptyCallbackInstance();
        assertThat("Should not be canceled", callback.isCanceled(), is(false));
        assertThat("Should not be complete", callback.isComplete(), is(false));

        callback.onComplete();

        assertThat("Should now be complete", callback.isComplete(), is(true));
        assertThat("Should not be canceled", callback.isCanceled(), is(false));
    }

    @Test
    public void shouldNotChangeStateFromCompleteToCancel() {
        final PatternMatchingStartupResultCallback callback = makeEmptyCallbackInstance();
        assertThat("Should not be canceled", callback.isCanceled(), is(false));
        assertThat("Should not be complete", callback.isComplete(), is(false));

        callback.onComplete();
        callback.cancel();

        assertThat("Should now be complete", callback.isComplete(), is(true));
        assertThat("Should not be canceled", callback.isCanceled(), is(false));
    }

    @Test
    public void shouldNotChangeStateFromCancelToComplete() {
        final PatternMatchingStartupResultCallback callback = makeEmptyCallbackInstance();
        assertThat("Should not be canceled", callback.isCanceled(), is(false));
        assertThat("Should not be complete", callback.isComplete(), is(false));

        callback.cancel();
        callback.onComplete();

        assertThat("Should now be canceled", callback.isCanceled(), is(true));
        assertThat("Should not be complete", callback.isComplete(), is(false));
    }

    @Test
    public void shouldNotBeCompleteWithoutMatch() {
        final PatternMatchingStartupResultCallback callback = makeWithPattern("foo");

        callback.onNext(frame("bar"));

        assertThat("Should not be complete", callback.isComplete(), is(false));
    }

    @Test(expected = OrchestrationException.class)
    public void shouldThrowOnCompleteWithoutMatch() {
        final PatternMatchingStartupResultCallback callback = makeWithPattern("foo");

        callback.onComplete();
    }

    @Test
    public void shouldBeCompleteWhenMatching() {
        final PatternMatchingStartupResultCallback callback = makeWithPattern("foo");

        callback.onNext(frame("foo"));

        assertThat("Should now be complete", callback.isComplete(), is(true));
    }

    @Test(expected = OrchestrationException.class)
    public void shouldThrowOnTimeout() {
        final PatternMatchingStartupResultCallback callback = makeWithPattern("foo", -1);

        callback.onNext(frame("bar"));
    }

    @Test
    public void shouldNotThrowIfAlreadyCanceled() {
        final PatternMatchingStartupResultCallback callback = makeWithPattern("foo", -1);
        callback.cancel();
        callback.onNext(frame("bar"));
    }


}
