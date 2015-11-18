package com.alexecollins.docker.orchestration.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VolumeFromTest {
    @Test
    public void idNoAccessMode() throws Exception {
        VolumeFrom volume = new VolumeFrom("xyz");
        assertEquals(new Id("xyz"), volume.getId());
        assertTrue(volume.isReadWrite());
        assertFalse(volume.isReadOnly());
    }

    @Test
    public void idWithAccessMode() throws Exception {
        VolumeFrom volumeRo = new VolumeFrom("xyz:ro");
        assertEquals(new Id("xyz"), volumeRo.getId());
        assertTrue(volumeRo.isReadOnly());
        assertFalse(volumeRo.isReadWrite());

        VolumeFrom volumeRw = new VolumeFrom("xyz:rw");
        assertEquals(new Id("xyz"), volumeRw.getId());
        assertFalse(volumeRw.isReadOnly());
        assertTrue(volumeRw.isReadWrite());
    }
}
