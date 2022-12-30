/*
     Copyright © 2020 - 2020 Chris Egerton <fearthecellos@gmail.com>
     This work is free. You can redistribute it and/or modify it under the
     terms of the Do What The Fuck You Want To Public License, Version 2,
     as published by Sam Hocevar. See the LICENSE file for more details.
*/

package com.github.c0urante.kafka.connect.sound.sink;

import com.github.c0urante.kafka.connect.sound.util.ConnectorThread;
import org.apache.kafka.connect.errors.ConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

class AudioWriteThread extends ConnectorThread {

    private static final Logger log = LoggerFactory.getLogger(AudioWriteThread.class);
    private static final int WRITE_QUEUE_CAPACITY = 10;

    private final Speakers speakers;
    private final AtomicReference<Throwable> failure;
    private final BlockingQueue<byte[]> readyToWrite;
    private final AtomicBoolean stopped;

    public AudioWriteThread(Speakers speakers) {
        this.speakers = speakers;
        this.failure = new AtomicReference<>(null);
        this.readyToWrite = new ArrayBlockingQueue<>(WRITE_QUEUE_CAPACITY);
        this.stopped = new AtomicBoolean(false);

        setDaemon(true);
    }

    @Override
    public void doRun() {
        try {
            log.info("Starting write thread");
            while (!stopped.get()) {
                byte[] samples;
                try {
                    samples = readyToWrite.take();
                } catch (InterruptedException e) {
                    log.debug("Write thread interrupted; will exit if task has been stopped", e);
                    continue;
                }
                speakers.play(samples);
            }
            log.info("Stopping write thread");
        } catch (Throwable t) {
            if (stopped.get()) {
                log.debug("Ignoring failure in write thread as task has already stopped", t);
            } else {
                failure.compareAndSet(null, new ConnectException("Unexpected exception in write thread", t));
            }
        }
    }

    public void put(byte[] samples) {
        try {
            this.readyToWrite.put(samples);
        } catch (InterruptedException e) {
            if (stopped.get()) {
                log.debug("Ignoring interruption while buffering samples since task is already shutting down");
            } else {
                throw new ConnectException("Interrupted while buffering samples", e);
            }
        }
    }

    public void checkForFailure() {
        Throwable t = failure.get();
        if (t != null) {
            throw new ConnectException("Audio write thread failed", t);
        }
    }

    public void shutDown() throws InterruptedException {
        if (stopped.getAndSet(true))
            return;

        speakers.shutDown();
        interrupt();
        join();
    }

}
