/*
     Copyright © 2020 - 2020 Chris Egerton <fearthecellos@gmail.com>
     This work is free. You can redistribute it and/or modify it under the
     terms of the Do What The Fuck You Want To Public License, Version 2,
     as published by Sam Hocevar. See the LICENSE file for more details.
*/

package com.github.c0urante.kafka.connect.sound.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

class LoggingContext {

    private static final Logger log = LoggerFactory.getLogger(LoggingContext.class);

    public static final String CONNECTOR_CONTEXT_KEY
            = "connector.context";

    public static LoggingContext captureCurrentContext() {
        String connectorContext = null;
        try {
            connectorContext = MDC.get(CONNECTOR_CONTEXT_KEY);
        } catch (Exception e) {
            log.warn("Unable to capture current logging context; using empty context instead", e);
        }
        return new LoggingContext(connectorContext);
    }

    private final String connectorContext;

    private LoggingContext(String connectorContext) {
        this.connectorContext = connectorContext;
    }

    public void apply() {
        if (connectorContext != null)
            MDC.put(CONNECTOR_CONTEXT_KEY, connectorContext);
    }

}
