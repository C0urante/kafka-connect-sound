/*
     Copyright © 2020 - 2020 Chris Egerton <fearthecellos@gmail.com>
     This work is free. You can redistribute it and/or modify it under the
     terms of the Do What The Fuck You Want To Public License, Version 2,
     as published by Sam Hocevar. See the LICENSE file for more details.
*/

package com.github.c0urante.kafka.connect.sound.sink;

import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.errors.ConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.Arrays;

class Speakers {

    private static final Logger log = LoggerFactory.getLogger(Speakers.class);

    private final SourceDataLine audioOutput;

    public Speakers(AudioFormat format, int bufferSize) {
        this.audioOutput = buildOutput(format, bufferSize);
    }

    public void play(byte[] samples) {
        log.trace("Playing samples: {}", Arrays.toString(samples));

        int bytesWritten = 0;
        while (bytesWritten < samples.length) {
            int bytesLeft = samples.length - bytesWritten;
            bytesWritten += audioOutput.write(samples, bytesWritten, bytesLeft);
        }

        log.trace("Finished playing samples ({} bytes)", samples.length);
    }

    public void shutDown() {
        log.debug("Shutting down");
        audioOutput.stop();
        audioOutput.close();
        log.debug("Shutdown complete");
    }

    private static SourceDataLine buildOutput(AudioFormat format, int bufferSize) {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            throw new ConfigException("Audio format not supported on this machine");
        }

        SourceDataLine result;
        try {
            result = (SourceDataLine) AudioSystem.getLine(info);
            result.open(format, bufferSize);
        } catch (LineUnavailableException e) {
            throw new ConnectException("Audio line not available", e);
        }

        result.start();
        log.debug("Created audio output with buffer size {} and format {}", bufferSize, format);
        return result;
    }

}
