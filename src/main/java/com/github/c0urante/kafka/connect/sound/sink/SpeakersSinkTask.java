/*
     Copyright © 2020 - 2020 Chris Egerton <fearthecellos@gmail.com>
     This work is free. You can redistribute it and/or modify it under the
     terms of the Do What The Fuck You Want To Public License, Version 2,
     as published by Sam Hocevar. See the LICENSE file for more details.
*/

package com.github.c0urante.kafka.connect.sound.sink;

import com.github.c0urante.kafka.connect.sound.SpeakersSinkConnectorConfig;
import com.github.c0urante.kafka.connect.sound.version.Version;

import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class SpeakersSinkTask extends SinkTask {

    private static final Logger log = LoggerFactory.getLogger(SpeakersSinkTask.class);

    private ByteBuffer buffer;
    private AudioWriteThread writeThread;

    @Override
    public void start(Map<String, String> props) {
        log.info("Starting task");
        SpeakersSinkConnectorConfig config = new SpeakersSinkConnectorConfig(props);

        int bufferSize = config.audio().bufferSize();

        this.buffer = ByteBuffer.allocate(bufferSize);

        Speakers speakers = new Speakers(config.audio().format(), bufferSize);
        this.writeThread = new AudioWriteThread(speakers);
        this.writeThread.start();
        log.info("Starting task");
    }

    @Override
    public void put(Collection<SinkRecord> records) {
        writeThread.checkForFailure();

        log.debug("Received {} records", records.size());
        long totalBytes = 0;
        for (SinkRecord record : records) {
            Object value = record.value();
            byte[] bytesValue;

            if (value instanceof Byte) {
                bytesValue = new byte[] { (Byte) value };
            } else if (value instanceof byte[]) {
                bytesValue = (byte[]) value;
            } else if (value instanceof ByteBuffer) {
                bytesValue = ((ByteBuffer) value).array();
            } else if (value == null) {
                throw new DataException("Record values must be non-null");
            } else {
                throw new DataException("Record values must be bytes, byte arrays, or ByteBuffers");
            }

            totalBytes += bytesValue.length;
            bufferSamples(bytesValue);
        }

        log.debug("Buffered {} total samples", totalBytes);
    }

    @Override
    public Map<TopicPartition, OffsetAndMetadata> preCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
        // Disable offset commits
        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        log.info("Shutting down task");
        try {
            writeThread.shutDown();
            log.info("Task finished shutdown");
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for write thread to shut down", e);
        }
    }

    @Override
    public String version() {
        return Version.get();
    }

    private void bufferSamples(byte[] samples) {
        log.trace("Received {} samples in Kafka record", samples.length);

        int i;
        if (samples.length >= buffer.remaining()) {
            log.trace("Number of samples ({}) is greater than or equal to remaining space ({}) in buffer", samples.length, buffer.remaining());
            i = buffer.remaining();
            buffer.put(samples, 0, buffer.remaining());
            putBufferedSamples();
        } else {
            i = 0;
        }

        while (samples.length - i >= buffer.capacity()) {
            log.trace("Number of remaining samples ({}) is greater than or equal to capacity ({}) of buffer", samples.length - i, buffer.capacity());
            buffer.put(samples, i, buffer.capacity());
            putBufferedSamples();
            i += buffer.capacity();
        }

        if (i < samples.length) {
            log.trace("Buffering {} remaining samples", samples.length - i);
            buffer.put(samples, i, samples.length - i);
            putBufferedSamples();
        }
    }

    private void putBufferedSamples() {
        byte[] bufferedSamples = buffer.array();
        // Have to clone the contents of the buffer in order to prevent subsequent buffering from mutating it
        byte[] samplesClone = new byte[buffer.position()];
        System.arraycopy(bufferedSamples, 0, samplesClone, 0, samplesClone.length);
        writeThread.put(samplesClone);
        buffer.clear();
    }

}
