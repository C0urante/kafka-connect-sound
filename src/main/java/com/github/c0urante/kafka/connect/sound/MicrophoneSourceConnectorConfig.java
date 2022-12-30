/*
     Copyright © 2020 - 2020 Chris Egerton <fearthecellos@gmail.com>
     This work is free. You can redistribute it and/or modify it under the
     terms of the Do What The Fuck You Want To Public License, Version 2,
     as published by Sam Hocevar. See the LICENSE file for more details.
*/

package com.github.c0urante.kafka.connect.sound;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static org.apache.kafka.common.config.ConfigDef.Importance;
import static org.apache.kafka.common.config.ConfigDef.Type;

public class MicrophoneSourceConnectorConfig extends AbstractConfig {

    public static final String TOPIC_CONFIG = "topic";
    public static final String TOPIC_DOC = "The Kafka topic to write to.";

    public static final String PARTITION_CONFIG = "partition";
    public static final int PARTITION_DEFAULT = 0;
    public static final String PARTITION_DOC = "The partition of the Kafka topic to write to";

    public static ConfigDef configDef() {
        ConfigDef result = new ConfigDef()
                .define(
                        TOPIC_CONFIG,
                        Type.STRING,
                        Importance.HIGH,
                        TOPIC_DOC
                ).define(
                        PARTITION_CONFIG,
                        Type.INT,
                        PARTITION_DEFAULT,
                        Importance.LOW,
                        PARTITION_DOC
                );
        return AudioConfig.define(result);
    }

    private final AudioConfig audioConfig;

    public MicrophoneSourceConnectorConfig(Map<?, ?> props) {
        super(configDef(), props);
        this.audioConfig = new AudioConfig(props);
    }

    public AudioConfig audio() {
        return audioConfig;
    }

    public String topic() {
        return getString(TOPIC_CONFIG);
    }

    public static void main(String[] args) throws IOException {
        OutputStream out;
        if (args.length == 1 && !args[0].equals("-")) {
            out = Files.newOutputStream(Paths.get(args[0]));
        } else if (args.length <= 1) {
            out = System.out;
        } else {
            System.err.printf("Usage: %s [<file>]%n", MicrophoneSourceConnectorConfig.class.getSimpleName());
            System.exit(1);
            return; // Only here to make the compiler happy
        }

        try (PrintWriter writer = new PrintWriter(out)) {
            writer.write(configDef().toEnrichedRst());
            writer.flush();
        }
    }

}

