/*
     Copyright © 2020 - 2020 Chris Egerton <fearthecellos@gmail.com>
     This work is free. You can redistribute it and/or modify it under the
     terms of the Do What The Fuck You Want To Public License, Version 2,
     as published by Sam Hocevar. See the LICENSE file for more details.
*/

package com.github.c0urante.kafka.connect.sound;

import com.github.c0urante.kafka.connect.sound.version.Version;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.SourceConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MicrophoneSourceConnector extends SourceConnector {

    private static final Logger log = LoggerFactory.getLogger(MicrophoneSourceConnector.class);

    private Map<String, String> configProps;

    @Override
    public void start(Map<String, String> props) {
        configProps = new HashMap<>(props);
    }

    @Override
    public Class<? extends Task> taskClass() {
        return MicrophoneSourceTask.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        return IntStream.range(0, maxTasks)
            .mapToObj(i -> new HashMap<>(configProps))
            .collect(Collectors.toList());
    }

    @Override
    public void stop() {
        configProps = null;
    }

    @Override
    public ConfigDef config() {
        return MicrophoneSourceConnectorConfig.CONFIG_DEF;
    }

    @Override
    public String version() {
        return Version.get();
    }
}
