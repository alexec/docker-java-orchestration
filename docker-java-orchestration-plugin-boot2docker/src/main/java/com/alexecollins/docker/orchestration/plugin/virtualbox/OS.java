package com.alexecollins.docker.orchestration.plugin.virtualbox;

class OS {

    static boolean isNotUnix() {
        String os = System.getProperty("os.name");
        return !os.contains("nix") && !os.contains("nux") && !os.contains("aix");
    }
}
