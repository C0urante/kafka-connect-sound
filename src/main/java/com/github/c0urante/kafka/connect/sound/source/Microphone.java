/*
     Copyright © 2020 - 2020 Chris Egerton <fearthecellos@gmail.com>
     This work is free. You can redistribute it and/or modify it under the
     terms of the Do What The Fuck You Want To Public License, Version 2,
     as published by Sam Hocevar. See the LICENSE file for more details.
*/

package com.github.c0urante.kafka.connect.sound.source;

import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.errors.ConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.util.Arrays;

class Microphone {

    private static final Logger log = LoggerFactory.getLogger(Microphone.class);

    private final TargetDataLine audioInput;
    private final int bufferSize;

    public Microphone(AudioFormat format, int bufferSize) {
        this.audioInput = buildInput(format, bufferSize);
        this.bufferSize = bufferSize;
    }

    public byte[] record() {
        byte[] buffer = new byte[bufferSize];
        int bytesRead = audioInput.read(buffer, 0, buffer.length);

        log.trace("Recorded samples ({} bytes)", bytesRead);

        if (bytesRead == buffer.length) {
            return buffer;
        } else {
            return Arrays.copyOfRange(buffer, 0, bytesRead);
        }
    }

    public void shutDown() {
        log.debug("Shutting down");
        audioInput.stop();
        audioInput.close();
        log.debug("Shutdown complete");
    }

    private static TargetDataLine buildInput(AudioFormat format, int bufferSize) {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            throw new ConfigException("Audio format not supported on this machine");
        }

        TargetDataLine result;
        try {
            result = (TargetDataLine) AudioSystem.getLine(info);
            result.open(format, bufferSize);
        } catch (LineUnavailableException e) {
            throw new ConnectException("Audio line not available", e);
        }

        result.start();
        log.debug("Created audio input with buffer size {} and format {}", bufferSize, format);
        return result;
    }

}
