package com.alexecollins.docker.orchestration;

import com.alexecollins.docker.orchestration.model.Conf;
import com.alexecollins.docker.orchestration.model.Id;
import com.alexecollins.docker.orchestration.util.PropertiesTokenResolver;
import com.alexecollins.docker.orchestration.util.TokenReplacingReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapLikeType;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

class Confs {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    static Reader replacingReader(File confFile, Properties properties) throws FileNotFoundException {
        return new TokenReplacingReader(new FileReader(confFile), new PropertiesTokenResolver(properties));
    }

    static Map<Id, Conf> read(File dockerConf, Properties properties) throws IOException {
        MapLikeType mapLikeType = MAPPER.getTypeFactory().constructMapType(LinkedHashMap.class, Id.class, Conf.class);
        return MAPPER.readValue(replacingReader(dockerConf, properties), mapLikeType);
    }
}
