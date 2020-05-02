/*
     Copyright © 2020 - 2020 Chris Egerton <fearthecellos@gmail.com>
     This work is free. You can redistribute it and/or modify it under the
     terms of the Do What The Fuck You Want To Public License, Version 2,
     as published by Sam Hocevar. See the LICENSE file for more details.
*/

package com.github.c0urante.kafka.connect.sound;

import com.github.c0urante.kafka.connect.sound.version.Version;

import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class SpeakersSinkTask extends SinkTask {

    private static final Logger log = LoggerFactory.getLogger(SpeakersSinkTask.class);

    private ByteBuffer buffer = ByteBuffer.allocate(1024);
    private Process outputProcess;

    @Override
    public void start(Map<String, String> props) {
        SpeakersSinkConnectorConfig config = new SpeakersSinkConnectorConfig(props);

        try {
            log.debug("Starting output process");
            outputProcess = new ProcessBuilder(
                    "play",
                    "-t", config.encoding(),
                    "--ignore-length",
                    "-"
                )
                .redirectOutput(config.stdout())
                .redirectError(config.stderr())
                .start();
        } catch (IOException e) {
            throw new ConnectException("Unable to build output process");
        }
    }

    @Override
    public void put(Collection<SinkRecord> records) {
        log.debug("Received {} records", records.size());
        for (SinkRecord record : records) {
            Object value = record.value();
            if (value instanceof Byte) {
                bufferSample((Byte) value);
            } else if (value instanceof Byte[] || value instanceof byte[]) {
                bufferSamples((byte[]) value);
            } else if (value instanceof ByteBuffer) {
                bufferSamples(((ByteBuffer) value).array());
            } else if (value == null) {
                throw new DataException("Record values must be non-null");
            } else {
                throw new DataException("Record values must be bytes, byte arrays, or ByteBuffers");
            }
        }
    }

    @Override
    public void stop() {
        log.debug("Destroying output process");
        outputProcess.destroy();
        outputProcess = null;
    }

    @Override
    public String version() {
        return Version.get();
    }

    @Override
    public Map<TopicPartition, OffsetAndMetadata> preCommit(
        Map<TopicPartition, OffsetAndMetadata> offsets
    ) {
        // TODO: Figure out a reliable way to resume playing from the middle of an audio stream.
        //       May involve automatically publishing headers on startup
        // For now, because we can't resume from the middle of an audio stream, no offsets will be
        // committed and every time the connector is restarted it'll start over again from the
        // beginning of each topic partition
        return Collections.emptyMap();
    }

    private void bufferSample(byte sample) {
        bufferSamples(new byte[] { sample });
    }

    private void bufferSamples(byte[] samples) {
        log.trace("Received {} samples in Kafka record", samples.length);

        int i;
        if (samples.length >= buffer.remaining()) {
            log.trace("Number of samples ({}) is greater than or equal to remaining space ({}) in buffer", samples.length, buffer.remaining());
            i = buffer.remaining();
            buffer.put(samples, 0, buffer.remaining());
            writeSamples(buffer.array());
            buffer.clear();
        } else {
            i = 0;
        }

        while (samples.length - i >= buffer.capacity()) {
            log.trace("Number of remaining samples ({}) is greater than or equal to capacity ({}) of buffer", samples.length - i, buffer.capacity());
            buffer.put(samples, i, buffer.capacity());
            writeSamples(buffer.array());
            buffer.clear();
            i += buffer.capacity();
        }

        if (i < samples.length) {
            log.trace("Buffering {} remaining samples", samples.length - i);
            buffer.put(samples, i, samples.length - i);
        }
    }

    private void writeSamples(byte[] samples) {
        log.trace("Writing samples: {}", Arrays.toString(samples));
        try {
            outputProcess.getOutputStream().write(samples);
        } catch (IOException e) {
            throw new ConnectException("Failed to write to output stream", e);
        }
    }
}
