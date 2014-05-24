package com.alexecollins.docker.orchestration.util;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.Scanner;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class FiltersTest {

	private File dir;
	private File file;

	@Before
	public void setUp() throws Exception {
		dir = new File("target/test/filters");
		assert dir.isDirectory() || dir.mkdirs();

		file = new File(dir, "test.txt");
		new FileOutputStream(file).write("test ${project.version}\n".getBytes());
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

		assertThat(new Scanner(file).useDelimiter("\\A").next(), equalTo("test 1.0.0\n"));
	}

	@Test
	public void testMaxLength() throws Exception {
		final Properties p = new Properties();
		p.setProperty("a", "1");
		p.setProperty("aa", "1");

		assertEquals(2, Filters.maxKeyLength(p));

	}
}