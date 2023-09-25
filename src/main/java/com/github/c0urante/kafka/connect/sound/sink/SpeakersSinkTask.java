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

    private Speakers speakers;

    @Override
    public void start(Map<String, String> props) {
        log.info("Starting task");
        SpeakersSinkConnectorConfig config = new SpeakersSinkConnectorConfig(props);

        int bufferSize = config.audio().bufferSize();

        this.speakers = new Speakers(config.audio().format(), bufferSize);
        log.info("Starting task");
    }

    @Override
    public void put(Collection<SinkRecord> records) {
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
            speakers.play(bytesValue);
        }

        log.debug("Wrote {} total samples", totalBytes);
    }

    @Override
    public Map<TopicPartition, OffsetAndMetadata> preCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
        // Disable offset commits
        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        log.info("Shutting down task");
        speakers.shutDown();
        log.info("Task finished shutdown");
    }

    @Override
    public String version() {
        return Version.get();
    }

}
