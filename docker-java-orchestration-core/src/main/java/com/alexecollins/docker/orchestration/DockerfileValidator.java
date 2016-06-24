package com.alexecollins.docker.orchestration;

import com.github.dockerjava.core.GoLangFileMatch;
import com.github.dockerjava.core.exception.GoLangFileMatchException;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dockerfile validator : validate the file format
 */
class DockerfileValidator {

    // Some regexes sourced from:
    // http://stackoverflow.com/a/2821201/1216976
    // http://stackoverflow.com/a/3809435/1216976
    // http://stackoverflow.com/a/6949914/1216976
    private final static Map<String, Pattern> INSTRUCTIONS_PATTERNS = instructionsPatterns();
    private static final Logger logger = LoggerFactory.getLogger(DockerfileValidator.class);

    private static Map<String, Pattern> instructionsPatterns() {
        Pattern addPattern = Pattern.compile("^(~?[${.}A-z0-9\\/_.-]+|https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*))\\s~?[A-z0-9\\/_.-]+$");
        Map<String, Pattern> instructionPatterns = new HashMap<>();
        instructionPatterns.put("FROM", Pattern.compile("^[${.}a-z0-9./_:-]+((:[${.}a-z0-9._-]+)?)$", Pattern.MULTILINE));
        instructionPatterns.put("MAINTAINER", Pattern.compile(".+"));
        instructionPatterns.put("EXPOSE", Pattern.compile("^[${.}A-z0-9]+([.${.}/a-zA-Z0-9\\s]+)?$"));
        instructionPatterns.put("ENV", Pattern.compile("^[${.}a-zA-Z_]+[${.}a-zA-Z0-9_]* .+$"));
        instructionPatterns.put("USER", Pattern.compile("^[${.}a-z_][${.}a-z0-9_]{0,30}$"));
        instructionPatterns.put("RUN", Pattern.compile(".+"));
        instructionPatterns.put("CMD", Pattern.compile(".+"));
        instructionPatterns.put("ONBUILD", Pattern.compile(".+"));
        instructionPatterns.put("ENTRYPOINT", Pattern.compile(".+"));
        instructionPatterns.put("ADD", addPattern);
        instructionPatterns.put("COPY", addPattern);
        instructionPatterns.put("VOLUME", Pattern.compile("^~?([${.}A-z0-9\\/_.-]+|\\[(\\s*)?(\"[A-z0-9\\/_. -]+\"(,\\s*)?)+(\\s*)?\\])$"));
        instructionPatterns.put("WORKDIR", Pattern.compile("^~?[${.}A-z0-9\\/_.-]+$"));
        return instructionPatterns;
    }


    void validate(File src) throws IOException {
        boolean isOnError = false;
        Preconditions.checkArgument(src.exists(),
                "Path %s doesn't exist", src);
        File dockerFile;
        if (src.isDirectory()) {
            Preconditions.checkState(new File(src, "Dockerfile").exists(),
                    "Dockerfile doesn't exist in " + src);

            dockerFile = new File(src, "Dockerfile");
        } else {
            Preconditions.checkState("Dockerfile".equals(src.getName()),
                    "Dockerfile isn't " + src);
            dockerFile = src;
            src = dockerFile.getParentFile();
        }

        List<String> dockerFileContent = FileUtils.readLines(dockerFile);

        if (dockerFileContent.size() <= 0) {
            throw new OrchestrationException(String.format(
                    "Dockerfile %s is empty", dockerFile));
        }

        File dockerIgnoreFile = new File(src, ".dockerignore");
        if (dockerIgnoreFile.exists()) {
            int lineNumber = 0;
            List<String> dockerIgnoreFileContent = FileUtils.readLines(dockerIgnoreFile);
            for (String pattern : dockerIgnoreFileContent) {
                lineNumber++;
                pattern = pattern.trim();
                if (pattern.isEmpty()) {
                    continue; // skip empty lines
                }
                pattern = FilenameUtils.normalize(pattern);
                try {
                    // validate pattern and make sure we aren't excluding Dockerfile
                    if (GoLangFileMatch.match(pattern, "Dockerfile")) {
                        logger.error(
                                String.format("Dockerfile is excluded by pattern '%s' on line %s in .dockerignore file", pattern, lineNumber));
                        isOnError = true;
                    }
                } catch (GoLangFileMatchException e) {
                    logger.error(String.format("Invalid pattern '%s' on line %s in .dockerignore file", pattern, lineNumber));
                    isOnError = true;
                }
            }
        }

        int lineNumber = 0;
        boolean fromCheck = false;
        String currentLine = "";
        for (String cmd : dockerFileContent) {

            lineNumber++;

            if (cmd.trim().isEmpty() || cmd.startsWith("#"))
                continue; // skip empty and comment lines

            currentLine += cmd;
            if (cmd.endsWith("\\")) {
                continue;
            }

            String instruction;
            String instructionParams;
            if (currentLine.trim().contains(" ")) {
                final String[] splitLine = currentLine.trim().split(" ", 2);
                instruction = splitLine[0];
                instructionParams = splitLine[1];
            } else {
                instruction = currentLine.trim();
                instructionParams = null;
            }

            // First instruction must be FROM
            if (!fromCheck) {
                fromCheck = true;
                if (!"FROM".equalsIgnoreCase(instruction)) {
                    logger.error(String.format(
                            "Missing or misplaced FROM on line [%d] of %s, found %s", lineNumber, dockerFile, currentLine));
                    isOnError = true;
                }
            } else {
                if ("FROM".equalsIgnoreCase(instruction)) {
                    logger.error(String.format(
                            "Missing or misplaced FROM on line [%d] of %s, found %s", lineNumber, dockerFile, currentLine));
                    isOnError = true;
                }
            }

            if (INSTRUCTIONS_PATTERNS.containsKey(instruction)) {
                if (instructionParams == null) {
                    logger.error(String.format(
                            "Missing param on line [%d] of %s, found %s", lineNumber, dockerFile, currentLine));
                    isOnError = true;
                } else {
                    Pattern instructionPattern = INSTRUCTIONS_PATTERNS.get(instruction);
                    Matcher curMatcher = instructionPattern.matcher(instructionParams);
                    if (!curMatcher.matches()) {
                        logger.error(String.format(
                                "Wrong %s format on line [%d] of %s, must match", currentLine, lineNumber, dockerFile));
                        isOnError = true;
                    }

                    if ("FROM".equalsIgnoreCase(instruction)) {
                        curMatcher = instructionPattern.matcher(instructionParams);

                        if (curMatcher.find()) {
                            String version = "";
                            if (curMatcher.groupCount() >= 1)
                                version = curMatcher.group(1);

                            if (version.length() == 0) {
                                logger.warn(String.format(
                                        "Provide a version and don't use latest version in FROM on line [%d] of %s, found %s", lineNumber, dockerFile, currentLine));
                            } else if (version.equals(":latest")) {
                                logger.warn(String.format(
                                        "Don't use latest version in FROM on line [%d] of %s, found %s", lineNumber, dockerFile, currentLine));

                            }

                        }

                    }

                    if ("RUN".equalsIgnoreCase(instruction)) {
                        if (instructionParams.length() > 100) {
                            String[] realLine = instructionParams.split("\\\\");
                            for (int i = 0; i < realLine.length; i++) {
                                if (realLine[i].length() > 100) {
                                    logger.warn(String.format(
                                            "The line %d of %s is too long (more than 100 chars) : %s...", (lineNumber - realLine.length + i + 1), dockerFile, realLine[i].substring(0, 50)));
                                }
                            }
                        }
                    }
                }

            } else {
                logger.error(String.format(
                        "Wrong instruction %s on line [%d] of %s", currentLine, lineNumber, dockerFile));
                isOnError = true;
            }

            //Deal with multi lines
            currentLine = "";

        }

        if (!Strings.isNullOrEmpty(currentLine)) {
            logger.error(String.format(
                    "Last instruction is not finish on line [%d] of %s, please remove the backslash", lineNumber, dockerFile));
            isOnError = true;

        }

        if (isOnError)
            throw new OrchestrationException(String.format("Error while validate Dockerfile %s.", dockerFile));

    }
}
