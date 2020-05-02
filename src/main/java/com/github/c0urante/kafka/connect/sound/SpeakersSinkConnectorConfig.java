/*
     Copyright © 2020 - 2020 Chris Egerton <fearthecellos@gmail.com>
     This work is free. You can redistribute it and/or modify it under the
     terms of the Do What The Fuck You Want To Public License, Version 2,
     as published by Sam Hocevar. See the LICENSE file for more details.
*/

package com.github.c0urante.kafka.connect.sound;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

public class SpeakersSinkConnectorConfig extends AbstractConfig {

    public static final String ENCODING_CONFIG = "encoding";
    public static final String ENCODING_DEFAULT = "wav";
    public static final String ENCODING_DOC = "The encoding schema for the audio.";

    public static final String PRINT_STDOUT_CONFIG = "print.stdout";
    public static final boolean PRINT_STDOUT_DEFAULT = false;
    public static final String PRINT_STDOUT_DOC =
        "Whether to print the stdout for the invoked process to the console";

    public static final String PRINT_STDERR_CONFIG = "print.stderr";
    public static final boolean PRINT_STDERR_DEFAULT = false;
    public static final String PRINT_STDERR_DOC =
        "Whether to print the stderr for the invoked process to the console";


    public static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(
                    ENCODING_CONFIG,
                    ConfigDef.Type.STRING,
                    ENCODING_DEFAULT,
                    ConfigDef.Importance.HIGH,
                    ENCODING_DOC
            ).define(
                    PRINT_STDOUT_CONFIG,
                    ConfigDef.Type.BOOLEAN,
                    PRINT_STDOUT_DEFAULT,
                    ConfigDef.Importance.LOW,
                    PRINT_STDOUT_DOC
            ).define(
                    PRINT_STDERR_CONFIG,
                    ConfigDef.Type.BOOLEAN,
                    PRINT_STDERR_DEFAULT,
                    ConfigDef.Importance.LOW,
                    PRINT_STDERR_DOC
            );

    public SpeakersSinkConnectorConfig(Map<?, ?> props) {
        super(CONFIG_DEF, props);
    }

    public String encoding() {
        return getString(ENCODING_CONFIG);
    }

    public ProcessBuilder.Redirect stdout() {
        return redirect(getBoolean(PRINT_STDOUT_CONFIG));
    }

    public ProcessBuilder.Redirect stderr() {
        return redirect(getBoolean(PRINT_STDERR_CONFIG));
    }

    private ProcessBuilder.Redirect redirect(boolean print) {
        return print ? ProcessBuilder.Redirect.INHERIT : ProcessBuilder.Redirect.PIPE;

    }


    public static void main(String[] args) throws FileNotFoundException {
        OutputStream out;
        if (args.length == 1 && !args[0].equals("-")) {
            out = new FileOutputStream(args[0]);
        } else if (args.length <= 1) {
            out = System.out;
        } else {
            System.err.printf("Usage: %s [<file>]%n", SpeakersSinkConnectorConfig.class.getSimpleName());
            System.exit(1);
            return; // Only here to make the compiler happy
        }

        try (PrintWriter writer = new PrintWriter(out)) {
            writer.write(CONFIG_DEF.toEnrichedRst());
            writer.flush();
        }
    }
}

