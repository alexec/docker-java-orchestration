package com.alexecollins.docker.orchestration;

import com.alexecollins.docker.orchestration.model.Id;
import com.github.dockerjava.core.GoLangFileMatch;
import com.github.dockerjava.core.GoLangFileMatchException;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Dockerfile validator : validate the file format 
 * TODO add check on best practice
 */
public class DockerfileValidator {

    private static Logger logger = Logger.getLogger(DockerfileValidator.class.getName());

    // Some regexes sourced from:
    // http://stackoverflow.com/a/2821201/1216976
    // http://stackoverflow.com/a/3809435/1216976
    // http://stackoverflow.com/a/6949914/1216976
    private final static HashMap<String, Pattern> instructionsParams = new HashMap<String, Pattern>();

    static {
        instructionsParams.put("FROM", Pattern.compile("^[a-z0-9.\\/_-]+(:[a-z0-9._-]+)?$", Pattern.MULTILINE ));
        instructionsParams.put("MAINTAINER", Pattern.compile(".+"));
        instructionsParams.put("EXPOSE", Pattern.compile("^[0-9]+([0-9\\s]+)?$"));
        instructionsParams.put("ENV", Pattern.compile("^[a-zA-Z_]+[a-zA-Z0-9_]* .+$"));
        instructionsParams.put("USER", Pattern.compile("^[a-z_][a-z0-9_]{0,30}$"));
        instructionsParams.put("RUN", Pattern.compile(".+"));
        instructionsParams.put("CMD", Pattern.compile(".+"));
        instructionsParams.put("ONBUILD", Pattern.compile(".+"));
        instructionsParams.put("ENTRYPOINT", Pattern.compile(".+"));
        instructionsParams.put("ADD", Pattern.compile("^(~?[A-z0-9\\/_.-]+|https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*))\\s~?[A-z0-9\\/_.-]+$"));
        instructionsParams.put("COPY", Pattern.compile("^(~?[A-z0-9\\/_.-]+|https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*))\\s~?[A-z0-9\\/_.-]+$"));
        instructionsParams.put("VOLUME", Pattern.compile("^~?([A-z0-9\\/_.-]+|\\[(\\s*)?(\"[A-z0-9\\/_. -]+\"(,\\s*)?)+(\\s*)?\\])$"));
        instructionsParams.put("WORKDIR", Pattern.compile("^~?[A-z0-9\\/_.-]+$"));
    }


    public static void validate(Id id, File src) throws IOException {
        boolean isOnError = false;
        Preconditions.checkArgument(src.exists(),
                "Path %s doesn't exist", src);
        File dockerFile;
        if(src.isDirectory()) {
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

        List<String> ignores = new ArrayList<String>();
        File dockerIgnoreFile = new File(src, ".dockerignore");
        if (dockerIgnoreFile.exists()) {
            int lineNumber = 0;
            List<String> dockerIgnoreFileContent = FileUtils.readLines(dockerIgnoreFile);
            for (String pattern: dockerIgnoreFileContent) {
                lineNumber++;
                pattern = pattern.trim();
                if (pattern.isEmpty()) {
                    continue; // skip empty lines
                }
                pattern = FilenameUtils.normalize(pattern);
                try {
                    // validate pattern and make sure we aren't excluding Dockerfile
                    if (GoLangFileMatch.match(pattern, "Dockerfile")) {
                        logger.severe(
                                String.format("Dockerfile is excluded by pattern '%s' on line %s in .dockerignore file", pattern, lineNumber));
                        isOnError = true;
                    }
                    ignores.add(pattern);
                } catch (GoLangFileMatchException e) {
                    logger.severe(String.format("Invalid pattern '%s' on line %s in .dockerignore file", pattern, lineNumber));
                    isOnError = true;
                }
            }
        }
        List<File> filesToAdd = new ArrayList<File>();
        filesToAdd.add(dockerFile);

        Map<String, String> environmentMap = new HashMap<String, String>();

        int lineNumber = 0;
        boolean fromCheck = false;
        String currentLine = "";
        for (String cmd : dockerFileContent) {

            lineNumber++;

            if (cmd.trim().isEmpty() || cmd.startsWith("#"))
                continue; // skip emtpy and commend lines

            currentLine += cmd;
            if(cmd.endsWith("\\")) {
                continue;
            }
            
            String instruction;
            String instructionParams;
            if(currentLine.trim().contains(" ")) {
                final String[] splitedLine = currentLine.trim().split(" ", 2);
                instruction = splitedLine[0];
                instructionParams = splitedLine[1];
            } else {
                instruction = currentLine.trim();
                instructionParams = null;
            }

            // First instruction must be FROM
            if (!fromCheck) {
                fromCheck = true;
                if (!"FROM".equalsIgnoreCase(instruction)) {
                    logger.severe(String.format(
                            "Missing or misplaced FROM on line [%d] of %s, found %s", lineNumber, dockerFile, currentLine));
                    isOnError = true;
                }
            } else {
                if ("FROM".equalsIgnoreCase(instruction)) {
                    logger.severe(String.format(
                            "Missing or misplaced FROM on line [%d] of %s, found %s", lineNumber, dockerFile, currentLine));
                    isOnError = true;
                }
            }


            if (instructionsParams.containsKey(instruction)) {
                if (! instructionsParams.get(instruction).matcher(instructionParams).matches()) {
                    logger.severe(String.format(
                            "Wrong %s format on line [%d] of %s", currentLine, lineNumber, dockerFile));
                    isOnError = true;
                }
            } else {
                logger.severe(String.format(
                        "Wrong instruction %s on line [%d] of %s", currentLine, lineNumber, dockerFile));
                isOnError = true;
            }
            
            //Deal with multi lines
            currentLine = "";

        }
        
        if(!Strings.isNullOrEmpty(currentLine)){
            logger.severe(String.format(
                    "Last instruction is not finish on line [%d] of %s, please remove the backslash", lineNumber, dockerFile));
            isOnError = true;
            
        }
        
        if(isOnError)
            throw new OrchestrationException(String.format("Error while validate Dockerfile %s.", dockerFile));

    }
}
