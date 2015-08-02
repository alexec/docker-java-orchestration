package com.alexecollins.docker.orchestration.model;

import lombok.Data;

import java.util.regex.Pattern;

@Data
public class LogPattern {
    private int timeout = 30 * 1000;
    private Pattern pattern;

    public LogPattern(String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    public LogPattern() {
    }
}
