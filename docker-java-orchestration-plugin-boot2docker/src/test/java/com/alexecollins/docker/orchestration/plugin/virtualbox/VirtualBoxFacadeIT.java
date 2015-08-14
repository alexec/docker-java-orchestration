package com.alexecollins.docker.orchestration.plugin.virtualbox;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Ignore("only to be run manually")
public class VirtualBoxFacadeIT {

    private static final int PORT = 14321;
    private final VirtualBoxFacade virtualBoxFacade = new VirtualBoxFacade();
    private String vmName;

    @Before
    public void setUp() throws Exception {
        vmName = new Boot2DockerVmNameFinder(virtualBoxFacade).getVmName();
    }

    @After
    public void tearDown() throws Exception {
        if (OS.isNotUnix()) {
            virtualBoxFacade.deletePortForward(vmName, PORT);
        }
    }

    @Test
    public void canCreateAndDeletePortForward() throws Exception {

        if (OS.isNotUnix()) {
            virtualBoxFacade.createPortForward(vmName, PORT);
            assertTrue(virtualBoxFacade.getPortForwards(vmName).contains(PORT));
            virtualBoxFacade.recreatePortForward(vmName, PORT);
            assertTrue(virtualBoxFacade.getPortForwards(vmName).contains(PORT));

            virtualBoxFacade.deletePortForward(vmName, PORT);
            assertFalse(virtualBoxFacade.getPortForwards(vmName).contains(PORT));
        }
    }
}