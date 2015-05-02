package com.alexecollins.docker.orchestration.model;


import lombok.Data;

import java.net.URI;
import java.util.regex.Pattern;

@Data
public class Ping {

    private URI url;
    private int timeout = 30 * 1000;
    private Pattern pattern = Pattern.compile(".*");
}
