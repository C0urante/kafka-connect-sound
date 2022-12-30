/*
     Copyright © 2020 - 2020 Chris Egerton <fearthecellos@gmail.com>
     This work is free. You can redistribute it and/or modify it under the
     terms of the Do What The Fuck You Want To Public License, Version 2,
     as published by Sam Hocevar. See the LICENSE file for more details.
*/

package com.github.c0urante.kafka.connect.sound.source;

import com.github.c0urante.kafka.connect.sound.MicrophoneSourceConnectorConfig;
import com.github.c0urante.kafka.connect.sound.version.Version;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MicrophoneSourceTask extends SourceTask {

    private static final Logger log = LoggerFactory.getLogger(MicrophoneSourceTask.class);

    private MicrophoneSourceConnectorConfig config;
    private AudioReadThread readThread;

    @Override
    public void start(Map<String, String> props) {
        log.info("Starting task");
        config = new MicrophoneSourceConnectorConfig(props);

        int bufferSize = config.audio().bufferSize();

        Microphone microphone = new Microphone(config.audio().format(), bufferSize);
        this.readThread = new AudioReadThread(microphone);

        this.readThread.start();
        log.info("Task finished startup");
    }

    @Override
    public List<SourceRecord> poll() {
        readThread.checkForFailure();
        List<TimestampedSamples> samples = readThread.drain();

        long totalBytes = samples.stream()
                .mapToLong(TimestampedSamples::size)
                .sum();

        List<SourceRecord> result = samples.stream()
            .map(this::record)
            .collect(Collectors.toList());

        log.trace("Returning {} records with {} total bytes from poll", result.size(), totalBytes);
        return result;
    }

    @Override
    public void stop() {
        log.info("Shutting down task");
        try {
            readThread.shutDown();
            log.info("Task finished shutdown");
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for write thread to shut down", e);
        }
    }

    @Override
    public String version() {
        return Version.get();
    }

    private SourceRecord record(TimestampedSamples timestampedSamples) {
        log.trace("Sending record of {} bytes", timestampedSamples.size());
        // Lol no source offsets
        return new SourceRecord(
                null, null,
                config.topic(), 0,
                null, null,
                Schema.BYTES_SCHEMA, timestampedSamples.samples,
                timestampedSamples.timestamp
        );
    }

}
