/*
     Copyright © 2020 - 2020 Chris Egerton <fearthecellos@gmail.com>
     This work is free. You can redistribute it and/or modify it under the
     terms of the Do What The Fuck You Want To Public License, Version 2,
     as published by Sam Hocevar. See the LICENSE file for more details.
*/

package com.github.c0urante.kafka.connect.sound.source;

import com.github.c0urante.kafka.connect.sound.util.ConnectorThread;
import org.apache.kafka.connect.errors.ConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

class AudioReadThread extends ConnectorThread {

    private static final Logger log = LoggerFactory.getLogger(AudioReadThread.class);
    private static final int READ_QUEUE_CAPACITY = 10;

    private final Microphone microphone;
    private final AtomicReference<Throwable> failure;
    private final BlockingQueue<TimestampedSamples> readyToWrite;
    private final AtomicBoolean stopped;

    public AudioReadThread(Microphone microphone) {
        this.microphone = microphone;
        this.failure = new AtomicReference<>(null);
        this.readyToWrite = new ArrayBlockingQueue<>(READ_QUEUE_CAPACITY);
        this.stopped = new AtomicBoolean(false);

        setDaemon(true);
    }

    @Override
    public void doRun() {
        try {
            log.info("Starting read thread");
            while (!stopped.get()) {
                byte[] samples = microphone.record();
                readyToWrite.put(TimestampedSamples.of(samples));
            }
            log.info("Stopping read thread");
        } catch (Throwable t) {
            if (stopped.get()) {
                log.debug("Ignoring failure in read thread as task has already stopped", t);
            } else {
                failure.compareAndSet(null, new ConnectException("Unexpected exception in read thread", t));
            }
        }
    }

    public List<TimestampedSamples> drain() {
        List<TimestampedSamples> result = new ArrayList<>();
        readyToWrite.drainTo(result);
        return result;
    }

    public void checkForFailure() {
        Throwable t = failure.get();
        if (t != null) {
            throw new ConnectException("Audio read thread failed", t);
        }
    }

    public void shutDown() throws InterruptedException {
        if (stopped.getAndSet(true))
            return;

        microphone.shutDown();
        interrupt();
        join();
    }

}
