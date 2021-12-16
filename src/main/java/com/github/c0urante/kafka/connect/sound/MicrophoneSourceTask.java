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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class MicrophoneSourceTask extends SourceTask {

    private static final Logger log = LoggerFactory.getLogger(MicrophoneSourceTask.class);

    MicrophoneSourceConnectorConfig config;
    private volatile boolean stopped;
    private AtomicReference<ConnectException> readThreadFailure;
    private BlockingQueue<byte[]> readyToWrite;
    private Process inputProcess;
    private Thread readThread;

    @Override
    public void start(Map<String, String> props) {
        config = new MicrophoneSourceConnectorConfig(props);

        stopped = false;
        readThreadFailure = new AtomicReference<>();
        readyToWrite = new ArrayBlockingQueue<>(10);

        try {
            log.debug("Starting input process");
            inputProcess = new ProcessBuilder("rec", "-t", config.encoding(), "-")
                .redirectError(config.stderr())
                .start();
        } catch (IOException e) {
            throw new ConnectException("Unable to build input process");
        }

        log.debug("Starting read thread");
        readThread = new Thread(this::readLoop);
        readThread.setDaemon(true);
        readThread.start();
    }

    @Override
    public List<SourceRecord> poll() {
        List<byte[]> bufferedSamples = new ArrayList<>();
        readyToWrite.drainTo(bufferedSamples);
        List<SourceRecord> result = bufferedSamples.stream()
            .map(this::record)
            .collect(Collectors.toList());
        log.trace("Returning {} records from poll", result.size());
        return result;
    }

    @Override
    public void stop() {
        log.debug("Interrupting read thread");
        readThread.interrupt();

        log.debug("Destroying input process");
        try {
            inputProcess.destroy();
        } catch (Throwable t) {
            log.warn("Error while attempting to destroy input process", t);
        } finally {
            inputProcess = null;
        }
    }

    @Override
    public String version() {
        return Version.get();
    }

    private SourceRecord record(byte[] value) {
        log.trace("Sending record of size {}", value.length);
        // Lol no source offsets
        return new SourceRecord(null, null, config.topic(), 0, Schema.BYTES_SCHEMA, value);
    }

    private void readLoop() {
        try {
            while (!stopped) {
                byte[] samples = readSamples();
                readyToWrite.put(samples);
            }
        } catch (Throwable t) {
          if (stopped) {
              log.debug("Ignoring failure in read thread as task has already stopped", t);
          } else {
              readThreadFailure.compareAndSet(null, new ConnectException("Unexpected exception in read thread", t));
          }
        }
    }

    private byte[] readSamples() throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        bytesRead = inputProcess.getInputStream().read(buffer);

        if (bytesRead == buffer.length) {
            return buffer;
        } else {
            byte[] reducedSizeBuffer = new byte[bytesRead];
            System.arraycopy(buffer, 0, reducedSizeBuffer, 0, bytesRead);
            return reducedSizeBuffer;
        }
    }

}
