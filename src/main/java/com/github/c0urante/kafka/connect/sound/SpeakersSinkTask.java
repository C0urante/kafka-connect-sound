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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

public class SpeakersSinkTask extends SinkTask {

    private static final Logger log = LoggerFactory.getLogger(SpeakersSinkTask.class);

    private volatile boolean stopped;
    private AtomicReference<ConnectException> writeThreadFailure;
    private ByteBuffer buffer;
    private BlockingQueue<byte[]> readyToWrite;
    private Process outputProcess;
    private Thread writeThread;

    @Override
    public void start(Map<String, String> props) {
        SpeakersSinkConnectorConfig config = new SpeakersSinkConnectorConfig(props);

        stopped = false;
        writeThreadFailure = new AtomicReference<>();
        buffer = ByteBuffer.allocate(1024);
        readyToWrite = new ArrayBlockingQueue<>(10);

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

        log.debug("Starting write thread");
        writeThread = new Thread(this::writeLoop);
        writeThread.setDaemon(true);
        writeThread.start();
    }

    @Override
    public void put(Collection<SinkRecord> records) {
        ConnectException exception = writeThreadFailure.get();
        if (exception != null) {
            throw exception;
        }

        log.debug("Received {} records in put", records.size());
        for (SinkRecord record : records) {
            Object value = record.value();
            if (value instanceof Byte) {
                bufferSamples(new byte[] { (Byte) value });
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
        log.debug("Interrupting write thread");
        writeThread.interrupt();

        log.debug("Destroying output process");
        try {
            outputProcess.destroy();
        } catch (Throwable t) {
            log.warn("Error while attempting to destroy output process", t);
        } finally {
            outputProcess = null;
        }
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
        byte[] samples = buffer.array();
        // Have to clone the contents of the buffer in order to prevent subsequent buffering from mutating it
        byte[] samplesClone = new byte[samples.length];
        System.arraycopy(samples, 0, samplesClone, 0, samplesClone.length);
        try {
            readyToWrite.put(samplesClone);
        } catch (InterruptedException e) {
            // This shouldn't happen; if it does, safer to fail the task than to continue running
            throw new ConnectException("Interrupted while inserting samples into buffer queue");
        }
        buffer.clear();
    }

    private void writeLoop() {
        try {
            while (!stopped) {
                byte[] samples;
                try {
                    samples = readyToWrite.take();
                } catch (InterruptedException e) {
                    log.debug("Write thread interrupted; will exit if task has been stopped", e);
                    continue;
                }
                playSamples(samples);
            }
        } catch (Throwable t) {
          if (stopped) {
              log.debug("Ignoring failure in write thread as task has already stopped", t);
          } else {
              writeThreadFailure.compareAndSet(null, new ConnectException("Unexpected exception in write thread", t));
          }
        }
    }

    private void playSamples(byte[] samples) throws IOException {
        log.trace("Playing samples: {}", Arrays.toString(samples));
        outputProcess.getOutputStream().write(samples);
    }

}
