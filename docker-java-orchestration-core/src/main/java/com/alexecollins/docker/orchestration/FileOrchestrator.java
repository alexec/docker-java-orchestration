package com.alexecollins.docker.orchestration;

import com.alexecollins.docker.orchestration.model.Conf;
import com.alexecollins.docker.orchestration.model.Id;
import com.alexecollins.docker.orchestration.model.Item;
import com.alexecollins.docker.orchestration.util.Filters;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Properties;

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
        copyDirectoryToDirectory(dockerFolder, destDir);

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
            final Path targetPath;
            if (destDir.isDirectory()) {
                targetPath = destDir.toPath().resolve(fileEntry.toPath().getFileName());
            } else {
                targetPath = destDir.toPath();
            }

            Files.copy(fileEntry.toPath(), targetPath,
                    StandardCopyOption.COPY_ATTRIBUTES,
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void copyDirectoryToDirectory(final File sourceDirectory, final File destinationDirectory) throws IOException {
        Files.walkFileTree(
                sourceDirectory.toPath(),
                ImmutableSet.of(FileVisitOption.FOLLOW_LINKS),
                1000,
                new CopyFileVisitor(destinationDirectory.toPath()));
    }

    private static class CopyFileVisitor extends SimpleFileVisitor<Path> {
        private final Path targetPath;
        private Path sourcePath = null;

        public CopyFileVisitor(Path targetPath) {
            this.targetPath = targetPath;
        }

        @Override
        public FileVisitResult preVisitDirectory(final Path dir,
                                                 final BasicFileAttributes attrs) throws IOException {
            if (sourcePath == null) {
                sourcePath = dir;
            }

            Files.createDirectories(targetPath.resolve(sourcePath
                        .relativize(dir)));
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(final Path file,
                                         final BasicFileAttributes attrs) throws IOException {
            Files.copy(file,
                    targetPath.resolve(sourcePath.relativize(file)),
                    StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
            return FileVisitResult.CONTINUE;
        }
    }

}
