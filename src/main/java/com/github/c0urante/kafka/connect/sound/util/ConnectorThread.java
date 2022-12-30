/*
     Copyright © 2020 - 2020 Chris Egerton <fearthecellos@gmail.com>
     This work is free. You can redistribute it and/or modify it under the
     terms of the Do What The Fuck You Want To Public License, Version 2,
     as published by Sam Hocevar. See the LICENSE file for more details.
*/

package com.github.c0urante.kafka.connect.sound.util;

/**
 * A special {@link Thread} subclass that automatically preserves the connector's
 * {@link org.slf4j.MDC MDC context}, if one is available at the time the thread is
 * created.
 */
public abstract class ConnectorThread extends Thread {

    private final LoggingContext loggingContext;

    public ConnectorThread() {
        this.loggingContext = LoggingContext.captureCurrentContext();
    }

    @Override
    public final void run() {
        loggingContext.apply();
        doRun();
    }

    protected abstract void doRun();

}
