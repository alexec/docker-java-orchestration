package com.alexecollins.docker.orchestration;

import com.alexecollins.docker.orchestration.model.Conf;
import com.alexecollins.docker.orchestration.model.Id;
import com.alexecollins.docker.orchestration.model.Item;
import com.alexecollins.docker.orchestration.util.Filters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Properties;

import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.FileUtils.copyDirectoryToDirectory;
import static org.apache.commons.io.FileUtils.copyFileToDirectory;

class FileOrchestrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileOrchestrator.class);

    /**
     * files to do property filtering on
     */
    private final FileFilter filter;
    /**
     * properties to filter
     */
    private final Properties properties;

    /**
     * output directory
     */
    private final File workDir;

    /**
     * root directory from which paths stem
     */
    private final File rootDir;

    FileOrchestrator(File workDir, File rootDir, FileFilter fileFilter, Properties properties) {
        if (workDir == null) {
            throw new IllegalArgumentException("Working output directory is null");
        }
        if (rootDir == null) {
            throw new IllegalArgumentException("Root project directory is null");
        }
        if (fileFilter == null) {
            throw new IllegalArgumentException("filter is null");
        }
        if (properties == null) {
            throw new IllegalArgumentException("properties is null");
        }

        this.workDir = workDir;
        this.rootDir = rootDir;
        this.filter = fileFilter;
        this.properties = properties;
    }

    File prepare(Id id, File dockerFolder, Conf conf) throws IOException {
        if (id == null) {
            throw new IllegalArgumentException("id is null");
        }
        final File destDir = new File(workDir, dockerFolder.getName());
        // copy template
        copyDirectory(dockerFolder, destDir);

        Filters.filter(destDir, filter, properties);

        // copy files
        for (Item item : conf.getPackaging().getAdd()) {
            File fileEntry = new File(rootDir, item.getPath());
            copyFileEntry(destDir, fileEntry);
            if (item.shouldFilter()) {
                Filters.filter(fileEntry, filter, properties);
            }
        }

        return destDir;
    }

    private void copyFileEntry(final File destDir, File fileEntry) throws IOException {
        LOGGER.info(" - add " + fileEntry);
        if (fileEntry.isDirectory()) {
            copyDirectoryToDirectory(fileEntry, destDir);
        } else {
            copyFileToDirectory(fileEntry, destDir);
        }
    }

}
