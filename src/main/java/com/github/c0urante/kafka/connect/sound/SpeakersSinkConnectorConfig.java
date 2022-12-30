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

public class SpeakersSinkConnectorConfig extends AbstractConfig {

    public static ConfigDef configDef() {
        ConfigDef result = new ConfigDef();
        return AudioConfig.define(result);
    }

    private final AudioConfig audioConfig;

    public SpeakersSinkConnectorConfig(Map<?, ?> props) {
        super(configDef(), props);
        this.audioConfig = new AudioConfig(props);
    }

    public AudioConfig audio() {
        return audioConfig;
    }

    public static void main(String[] args) throws IOException {
        OutputStream out;
        if (args.length == 1 && !args[0].equals("-")) {
            out = Files.newOutputStream(Paths.get(args[0]));
        } else if (args.length <= 1) {
            out = System.out;
        } else {
            System.err.printf("Usage: %s [<file>]%n", SpeakersSinkConnectorConfig.class.getSimpleName());
            System.exit(1);
            return; // Only here to make the compiler happy
        }

        try (PrintWriter writer = new PrintWriter(out)) {
            writer.write(configDef().toEnrichedRst());
            writer.flush();
        }
    }

}

