package com.alexecollins.docker.orchestration;

import com.alexecollins.docker.orchestration.model.Conf;
import com.alexecollins.docker.orchestration.model.Id;
import com.alexecollins.docker.orchestration.util.Filters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.FileUtils.copyDirectoryToDirectory;
import static org.apache.commons.io.FileUtils.copyFileToDirectory;

public class FileOrchestrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileOrchestrator.class);

    /**
     * files to do property filtering on
     */
    private final FileFilter filter;
    /**
     * properties to filter
     */
    private final Properties properties;

    protected FileOrchestrator(FileFilter fileFilter, Properties properties) {
        if (fileFilter == null) {
            throw new IllegalArgumentException("filter is null");
        }
        if (properties == null) {
            throw new IllegalArgumentException("properties is null");
        }

        this.filter = fileFilter;
        this.properties = properties;
    }

    protected File prepare(Id id, File dockerFolder, Conf conf, File workDir) throws IOException {
        if (id == null) {
            throw new IllegalArgumentException("id is null");
        }
        final File destDir = new File(workDir, dockerFolder.getName());
        // copy template
        copyDirectory(dockerFolder, destDir);

        Filters.filter(destDir, filter, properties);

        // copy files
		for (String file : conf.getPackaging().getAdd()) {
			File fileEntry = new File(filter(file));
			copyFileEntry(destDir, fileEntry);
			Filters.filter(fileEntry, filter, properties);
		}

        return destDir;
    }

    private String filter(String file) {
		for (Map.Entry<Object, Object> e : properties.entrySet()) {
			file = file.replace("${" + e.getKey() + "}", e.getValue().toString());
		}
		return file;
	}

    private void copyFileEntry(final File destDir, File fileEntry) throws IOException {
        if (fileEntry.isDirectory()) {
            LOGGER.info(" - add (dir) " + fileEntry.getAbsolutePath());
            copyDirectoryToDirectory(fileEntry, destDir);
        } else {
            LOGGER.info(" - add (file) " + fileEntry.getAbsolutePath());
            copyFileToDirectory(fileEntry, destDir);
        }
    }
}
