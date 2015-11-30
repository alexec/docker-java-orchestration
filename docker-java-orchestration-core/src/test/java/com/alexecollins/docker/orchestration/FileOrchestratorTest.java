package com.alexecollins.docker.orchestration;

import com.alexecollins.docker.orchestration.model.Conf;
import com.alexecollins.docker.orchestration.model.Id;
import com.alexecollins.docker.orchestration.model.Item;
import com.alexecollins.docker.orchestration.model.Packaging;
import com.alexecollins.docker.orchestration.util.TextFileFilter;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author marcus
 * @since 1.0.0
 */
public class FileOrchestratorTest {

    private static final File BASE_DIRECTORY =  FileUtils.getFile("target", "test-classes", "file_orchestrator");

    @Test
    public void testSimpleSourceDirectory() throws IOException {
        final File dockerFolder = new File(BASE_DIRECTORY, "simpleSourceDirectory");
        final File workDir = Files.createTempDir();

        prepare(dockerFolder, workDir);

        final File simpleSourceDirectory = new File(workDir, "simpleSourceDirectory");
        assertThat(simpleSourceDirectory.exists(), is(true));
        assertThat(simpleSourceDirectory.isDirectory(), is(true));

        final File testFile = new File(simpleSourceDirectory, "test.txt");
        assertThat(testFile.exists(), is(true));
        assertThat(testFile.isFile(), is(true));

        workDir.deleteOnExit();
    }

    @Test
    public void testSourceDirectoryWithSubdirectory() throws IOException {
        final File dockerFolder = new File(BASE_DIRECTORY, "sourceDirectoryWithSubdirectory");
        final File workDir = Files.createTempDir();

        prepare(dockerFolder, workDir);

        final File sourceDirectoryWithSubdirectory = new File(workDir, "sourceDirectoryWithSubdirectory");
        assertThat(sourceDirectoryWithSubdirectory.exists(), is(true));
        assertThat(sourceDirectoryWithSubdirectory.isDirectory(), is(true));

        final File subDirectory = new File(sourceDirectoryWithSubdirectory, "subDirectory");
        assertThat(subDirectory.exists(), is(true));
        assertThat(subDirectory.isDirectory(), is(true));

        final File testFile = new File(subDirectory, "test2.txt");
        assertThat(testFile.exists(), is(true));
        assertThat(testFile.isFile(), is(true));

        workDir.deleteOnExit();
    }

    @Test
    public void testAddFileToSimpleDirectory() throws IOException {
        final File dockerFolder = new File(BASE_DIRECTORY, "simpleSourceDirectory");
        final File workDir = Files.createTempDir();

        final List<Item> adds = Collections.singletonList(new Item(FileUtils.getFile(
              "sourceDirectoryWithSubdirectory","subDirectory","test2.txt").getPath()));

        prepare(dockerFolder, workDir, adds);

        final File simpleSourceDirectory = new File(workDir, "simpleSourceDirectory");
        assertThat(simpleSourceDirectory.exists(), is(true));
        assertThat(simpleSourceDirectory.isDirectory(), is(true));

        final File testFile = new File(simpleSourceDirectory, "test.txt");
        assertThat(testFile.exists(), is(true));
        assertThat(testFile.isFile(), is(true));

        final File test2File = new File(simpleSourceDirectory, "test2.txt");
        assertThat(test2File.exists(), is(true));
        assertThat(test2File.isFile(), is(true));

        workDir.deleteOnExit();
    }

    @Test
    public void testAddDirectoryToSimpleDirectory() throws IOException {
        final File dockerFolder = new File(BASE_DIRECTORY, "simpleSourceDirectory");
        final File workDir = Files.createTempDir();

        final List<Item> adds = Collections.singletonList(new Item(FileUtils.getFile(
                "sourceDirectoryWithSubdirectory","subDirectory").getPath()));

        prepare(dockerFolder, workDir, adds);

        final File simpleSourceDirectory = new File(workDir, "simpleSourceDirectory");
        assertThat(simpleSourceDirectory.exists(), is(true));
        assertThat(simpleSourceDirectory.isDirectory(), is(true));

        final File subDirectory = new File(simpleSourceDirectory, "subDirectory");
        assertThat(subDirectory.exists(), is(true));
        assertThat(subDirectory.isDirectory(), is(true));

        final File test2File = new File(subDirectory, "test2.txt");
        assertThat(test2File.exists(), is(true));
        assertThat(test2File.isFile(), is(true));

        workDir.deleteOnExit();
    }

    private void prepare(final File dockerFolder, final File workDir) throws IOException {
        prepare(dockerFolder,workDir,Collections.<Item>emptyList());
    }

    private void prepare(final File dockerFolder, final File workDir, final List<Item> adds) throws IOException {
        final Packaging packaging = Mockito.mock(Packaging.class);
        Mockito.when(packaging.getAdd()).thenReturn(adds);

        final Conf conf = Mockito.mock(Conf.class);
        Mockito.when(conf.getPackaging()).thenReturn( packaging);

        fileOrchestrator(dockerFolder, workDir)
                .prepare(new Id("test"), dockerFolder, conf);
    }

    private FileOrchestrator fileOrchestrator(final File dockerFolder, final File workDir) {
        return new FileOrchestrator(workDir, dockerFolder.getParentFile(), TextFileFilter.INSTANCE, new Properties());
    }

}
