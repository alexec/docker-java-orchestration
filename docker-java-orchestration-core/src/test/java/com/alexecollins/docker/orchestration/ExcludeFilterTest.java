package com.alexecollins.docker.orchestration;

import com.alexecollins.docker.orchestration.model.Conf;
import com.alexecollins.docker.orchestration.model.Id;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExcludeFilterTest {
    private static final Conf CONF = new Conf();

    @Test
    public void excludeNone() throws Exception {
        assertFalse(new ExcludeFilter().test(null, CONF));
    }

    @Test
    public void excludeOne() throws Exception {
        ExcludeFilter filter = new ExcludeFilter("app");
        assertFalse(filter.test(new Id("app"), CONF));
        assertTrue(filter.test(new Id("other"), CONF));
    }

    @Test
    public void excludeSame() throws Exception {
        ExcludeFilter filter = new ExcludeFilter("app", "other");
        assertFalse(filter.test(new Id("app"), CONF));
        assertFalse(filter.test(new Id("other"), CONF));
    }
}