/*
     Copyright © 2020 - 2020 Chris Egerton <fearthecellos@gmail.com>
     This work is free. You can redistribute it and/or modify it under the
     terms of the Do What The Fuck You Want To Public License, Version 2,
     as published by Sam Hocevar. See the LICENSE file for more details.
*/

package com.github.c0urante.kafka.connect.sound.source;

import java.util.function.LongSupplier;

class TimestampedSamples {
    public final byte[] samples;
    public final long timestamp;

    public static TimestampedSamples of(byte[] samples) {
        return of(samples, System::currentTimeMillis);
    }

    static TimestampedSamples of(byte[] samples, LongSupplier currentTime) {
        return new TimestampedSamples(samples, currentTime.getAsLong());
    }

    private TimestampedSamples(byte[] samples, long timestamp) {
        this.samples = samples;
        this.timestamp = timestamp;
    }

    public int size() {
        return samples.length;
    }

}
