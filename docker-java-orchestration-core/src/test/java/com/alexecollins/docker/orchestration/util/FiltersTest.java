package com.alexecollins.docker.orchestration.util;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class FiltersTest {

    private File dir;
    private String fileUnix;
    private String fileWindows;
    private Properties properties;

    @Before
    public void setUp() throws Exception {
        dir = new File("target/test/filters");
        assert dir.isDirectory() || dir.mkdirs();

        fileUnix = "unix.txt";
        writeFile(new File(dir, fileUnix), "test ${project.version}\n");
        fileWindows = "windows.txt";
        writeFile(new File(dir, fileWindows), "test ${project.version}\r\n");

        properties = new Properties();
        properties.setProperty("project.version", "1.0.0");
    }

    @Test
    public void testSimpleFilter() throws Exception {
        assertEquals("1.0.0", Filters.filter("${project.version}", properties));
    }

    @Test
    public void testFilter() throws Exception {

        Filters.filter(dir, new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return true;
            }
        }, properties);

        assertEquals("test 1.0.0\n", IOUtils.toString(new File(dir, fileUnix).toURI(), Charset.defaultCharset()));
        assertEquals("test 1.0.0\r\n", IOUtils.toString(new File(dir, fileWindows).toURI(), Charset.defaultCharset()));
    }

    @Test
    public void testMaxLength() throws Exception {
        final Properties p = new Properties();
        p.setProperty("a", "1");
        p.setProperty("aa", "1");

        assertEquals(2, Filters.maxKeyLength(p));

    }

    private void writeFile(File file, String data) throws IOException {
        FileWriter out = new FileWriter(file);
        try {
            IOUtils.write(data, out);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }
}