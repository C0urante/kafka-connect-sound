/*
     Copyright © 2020 - 2020 Chris Egerton <fearthecellos@gmail.com>
     This work is free. You can redistribute it and/or modify it under the
     terms of the Do What The Fuck You Want To Public License, Version 2,
     as published by Sam Hocevar. See the LICENSE file for more details.
*/

package com.github.c0urante.kafka.connect.sound;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.Config;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.config.ConfigValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.apache.kafka.common.config.ConfigDef.Importance;
import static org.apache.kafka.common.config.ConfigDef.Type;

public class AudioConfig extends AbstractConfig {

    private static final Logger log = LoggerFactory.getLogger(AudioConfig.class);

    public static final String SAMPLE_RATE_CONFIG = "sample.rate";
    public static final double SAMPLE_RATE_DEFAULT = 44100;
    public static final String SAMPLE_RATE_DOC = "Sample rate for the audio, in hertz";

    public static final String SAMPLE_SIZE_CONFIG = "sample.size";
    public static final int SAMPLE_SIZE_DEFAULT = 16;
    public static final String SAMPLE_SIZE_DOC = "Size of each sample, in bits";

    public static final String CHANNELS_COUNT_CONFIG = "channels.count";
    public static final int CHANNELS_COUNT_DEFAULT = 1;
    public static final String CHANNELS_COUNT_DOC = "Number of channels";

    public static final String ENDIAN_CONFIG = "endian";
    public static final String BIG_ENDIAN = "big";
    public static final String LITTLE_ENDIAN = "little";
    public static final String ENDIAN_DEFAULT = "little";
    public static final String ENDIAN_DOC = "Endianness of each sample. Permitted values are '"
            + BIG_ENDIAN + "' and '" + LITTLE_ENDIAN + "'";

    public static final String SIGNED_CONFIG = "signed";
    public static final boolean SIGNED_DEFAULT = true;
    public static final String SIGNED_DOC = "Whether samples are signed";

    public static final String BUFFER_SIZE_CONFIG = "buffer.size";
    public static final int BUFFER_SIZE_DEFAULT = 1024;
    public static final String BUFFER_SIZE_DOC = "Size in bytes of the buffer used to transfer audio to/from the OS";

    public static ConfigDef define(ConfigDef configDef) {
        return configDef
                .define(
                        SAMPLE_RATE_CONFIG,
                        Type.DOUBLE,
                        SAMPLE_RATE_DEFAULT,
                        Importance.MEDIUM,
                        SAMPLE_RATE_DOC
                ).define(
                        SAMPLE_SIZE_CONFIG,
                        Type.INT,
                        SAMPLE_SIZE_DEFAULT,
                        Importance.MEDIUM,
                        SAMPLE_SIZE_DOC
                ).define(
                        CHANNELS_COUNT_CONFIG,
                        Type.INT,
                        CHANNELS_COUNT_DEFAULT,
                        Importance.MEDIUM,
                        CHANNELS_COUNT_DOC
                ).define(
                        ENDIAN_CONFIG,
                        Type.STRING,
                        ENDIAN_DEFAULT,
                        Importance.MEDIUM,
                        ENDIAN_DOC
                ).define(
                        SIGNED_CONFIG,
                        Type.BOOLEAN,
                        SIGNED_DEFAULT,
                        Importance.MEDIUM,
                        SIGNED_DOC
                ).define(
                        BUFFER_SIZE_CONFIG,
                        Type.INT,
                        BUFFER_SIZE_DEFAULT,
                        ConfigDef.Range.atLeast(1),
                        Importance.LOW,
                        BUFFER_SIZE_DOC
                );
    }

    private static ConfigDef configDef() {
        return define(new ConfigDef());
    }

    private final float sampleRate;
    private final int sampleSize;
    private final int channelsCount;
    private final boolean signed;
    private final boolean bigEndian;
    private final int bufferSize;

    public AudioConfig(Map<?, ?> props) {
        super(configDef(), props);
        this.sampleRate = (float) (double) getDouble(SAMPLE_RATE_CONFIG);
        this.sampleSize = getInt(SAMPLE_SIZE_CONFIG);
        this.channelsCount = getInt(CHANNELS_COUNT_CONFIG);
        this.signed = true;
        this.bigEndian = BIG_ENDIAN.equalsIgnoreCase(getString(ENDIAN_CONFIG));
        this.bufferSize = getInt(BUFFER_SIZE_CONFIG);
    }

    public AudioFormat format() {
        return new AudioFormat(
                sampleRate,
                sampleSize,
                channelsCount,
                signed,
                bigEndian
        );
    }

    public int bufferSize() {
        return bufferSize;
    }

    public static void validateFormat(Map<String, String> props, Config config) {
        String connectorName = props.getOrDefault("name", "<unknown>");

        AudioConfig audioConfig;
        try {
            audioConfig = new AudioConfig(props);
        } catch (ConfigException e) {
            log.debug(
                    "Skipping multi-property validation of audio format for connector "
                            + connectorName + "since single-property validation has failed",
                    e
            );
            return;
        }

        try {
            audioConfig.format();
        } catch (Throwable t) {
            // Log the full stack trace here in case the message we include in our validation
            // logic is insufficient
            log.error("Invalid audio format for connector {}", connectorName, t);

            String message = "Invalid audio format";
            if (t.getMessage() != null)
                message += ": " + message;

            List<String> formatProperties = Arrays.asList(
                    SAMPLE_RATE_CONFIG,
                    SAMPLE_SIZE_CONFIG,
                    CHANNELS_COUNT_CONFIG,
                    ENDIAN_CONFIG,
                    SIGNED_CONFIG
            );

            for (String property : formatProperties) {
                addErrorMessage(property, message, config);
            }
        }
    }

    private static void addErrorMessage(String name, String message, Config config) {
        ConfigValue configValue = config.configValues().stream()
                .filter(cv -> cv.name().equals(name))
                .findAny()
                .orElse(null);

        if (configValue == null) {
            configValue = new ConfigValue(name);
            config.configValues().add(configValue);
        }

        configValue.addErrorMessage(message);
    }

}
