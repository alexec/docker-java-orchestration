package com.alexecollins.docker.orchestration;

import com.alexecollins.docker.orchestration.model.Conf;
import com.alexecollins.docker.orchestration.model.Id;
import org.junit.Test;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class ConfsTest {

    @Test
    public void readConf() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("test", "foo");

        Map<Id, Conf> confs = Confs.read(new File("src/test/resources/docker.yml"), properties);

        assertEquals(1, confs.size());

        Conf conf = confs.get(new Id("app"));

        assertEquals("foo", conf.getPackaging().getAdd().get(0).getPath());

    }
}