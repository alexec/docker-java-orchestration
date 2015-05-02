package com.alexecollins.docker.orchestration;

import com.alexecollins.docker.orchestration.model.Conf;

enum TagMatcher {
    ;

    static boolean matches(String tag, Conf conf) {
        return conf.hasImage() &&
                (
                        tag.equals(conf.getImage())
                                ||
                                !conf.getImage().contains(":") && tag.startsWith(conf.getImage() + ':')
                );
    }
}
