package com.alexecollins.docker.orchestration.util;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TextFileFilterTest {
    private final File dir = new File(System.getProperty("java.io.tmpdir"));

    @Test
    public void testTxtFile() throws Exception {
        File f = new File(dir, "test.txt");
        new FileOutputStream(f).write(new byte[0]);
        assertTrue(TextFileFilter.INSTANCE.accept(f));
    }

    @Test
    public void testXmlFile() throws Exception {
        File f = new File(dir, "test.xml");
        new FileOutputStream(f).write(new byte[0]);
        assertTrue(TextFileFilter.INSTANCE.accept(f));
    }

    @Test
    public void testBadXmlFile() throws Exception {
        File f = new File(dir, "testxml");
        new FileOutputStream(f).write(new byte[0]);
        assertFalse(TextFileFilter.INSTANCE.accept(f));
    }

    @Test
    public void testBinFile() throws Exception {
        File f = new File(dir, "test.bin");
        new FileOutputStream(f).write(new byte[0]);

        assertFalse(TextFileFilter.INSTANCE.accept(f));
    }
}