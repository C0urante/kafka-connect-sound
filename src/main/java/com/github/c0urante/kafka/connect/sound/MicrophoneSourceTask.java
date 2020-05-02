/*
     Copyright © 2020 - 2020 Chris Egerton <fearthecellos@gmail.com>
     This work is free. You can redistribute it and/or modify it under the
     terms of the Do What The Fuck You Want To Public License, Version 2,
     as published by Sam Hocevar. See the LICENSE file for more details.
*/

package com.github.c0urante.kafka.connect.sound;

import com.github.c0urante.kafka.connect.sound.version.Version;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MicrophoneSourceTask extends SourceTask {

    private static final Logger log = LoggerFactory.getLogger(MicrophoneSourceTask.class);

    MicrophoneSourceConnectorConfig config;
    private Process inputProcess;

    @Override
    public void start(Map<String, String> props) {
        config = new MicrophoneSourceConnectorConfig(props);

        try {
            log.debug("Starting input process");
            inputProcess = new ProcessBuilder("rec", "-t", config.encoding(), "-")
                .redirectError(config.stderr())
                .start();
        } catch (IOException e) {
            throw new ConnectException("Unable to build input process");
        }
    }

    @Override
    public List<SourceRecord> poll() {
        byte[] buffer = new byte[1024];
        int bytesRead;
        try {
            bytesRead = inputProcess.getInputStream().read(buffer);
        } catch (IOException e) {
            throw new ConnectException("Error while reading from stream", e);
        }

        byte[] value;
        if (bytesRead == buffer.length) {
            value = buffer;
        } else {
            value = new byte[bytesRead];
            System.arraycopy(buffer, 0, value, 0, bytesRead);
        }

        log.trace("Sending record of size {}", value.length);
        return Collections.singletonList(
            // Lol no source offsets
            new SourceRecord(null, null, config.topic(), 0, Schema.BYTES_SCHEMA, value)
        );
    }

    @Override
    public void stop() {
        log.debug("Destroying input process");
        inputProcess.destroy();
        inputProcess = null;
    }

    @Override
    public String version() {
        return Version.get();
    }
}
