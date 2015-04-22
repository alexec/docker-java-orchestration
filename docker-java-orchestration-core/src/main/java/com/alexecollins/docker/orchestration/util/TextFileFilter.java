package com.alexecollins.docker.orchestration.util;


import java.io.File;
import java.io.FileFilter;

/**
 * Filter text files.
 */
public class TextFileFilter implements FileFilter {
    public static final TextFileFilter INSTANCE = new TextFileFilter();

    private TextFileFilter() {
    }

    @Override
    public boolean accept(File pathname) {
        return pathname.isFile() && pathname.getName().matches("Dockerfile|.*\\.(cfg|conf|json|properties|sh|txt|xml|yaml|yml)");
    }
}
