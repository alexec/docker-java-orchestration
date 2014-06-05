package com.alexecollins.docker.orchestration.util;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FiltersTest {

	private File dir;
	private String file;

	@Before
	public void setUp() throws Exception {
		dir = new File("target/test/filters");
        file = "test.txt";
		assert dir.isDirectory() || dir.mkdirs();

		File testFile = new File(dir, file);
		FileOutputStream stream = new FileOutputStream(testFile);
        stream.write("test ${project.version}\n".getBytes());
        stream.close();
	}

	@Test
	public void testFilter() throws Exception {
		final Properties properties = new Properties();
		properties.setProperty("project.version", "1.0.0");

		Filters.filter(dir, new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return true;
			}
		}, properties);

        assertTrue(new Scanner(new File(dir, file)).useDelimiter("\\A").next().matches("^test 1.0.0\\s*$"));
	}

	@Test
	public void testMaxLength() throws Exception {
		final Properties p = new Properties();
		p.setProperty("a", "1");
		p.setProperty("aa", "1");

		assertEquals(2, Filters.maxKeyLength(p));

	}
}