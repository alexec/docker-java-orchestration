package com.alexecollins.docker.orchestration.plugin.virtualbox;

import com.alexecollins.docker.orchestration.model.Conf;
import org.junit.Before;
import org.junit.Test;

public class Boo2DockerPluginIT {

    private Boot2DockerPlugin boot2DockerPlugin;

    @Before
    public void setUp() throws Exception {
        boot2DockerPlugin = new Boot2DockerPlugin();
    }

    @Test
    public void quiet() {
        Conf conf = new Conf();
        conf.getPorts().add("6543");
        boot2DockerPlugin.started(null, conf);
    }
}