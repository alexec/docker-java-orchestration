package com.alexecollins.docker.orchestration.plugin.virtualbox;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VirtualBoxFacadeIT {

    private static final int PORT = 14321;
    private final VirtualBoxFacade virtualBoxFacade = new VirtualBoxFacade();

    @After
    public void tearDown() throws Exception {
        if (OS.isNotUnix()) {
            virtualBoxFacade.deletePortForward(PORT);
        }
    }

    @Test
    public void canCreateAndDeletePortForward() throws Exception {

        if (OS.isNotUnix()) {
            virtualBoxFacade.createPortForward(PORT);
            assertTrue(virtualBoxFacade.getPortForwards().contains(PORT));
            virtualBoxFacade.recreatePortForward(PORT);
            assertTrue(virtualBoxFacade.getPortForwards().contains(PORT));

            virtualBoxFacade.deletePortForward(PORT);
            assertFalse(virtualBoxFacade.getPortForwards().contains(PORT));
        }
    }
}