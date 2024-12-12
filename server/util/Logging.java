package com.chebuya.minegriefserver.util;

import java.util.logging.*;

public class Logging {
    public static final Logger LOGGER = Logger.getLogger(Logging.class.getName());

    public static void configureLogging(boolean DO_LOGGING) {
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter() {
            @Override
            public synchronized String format(LogRecord record) {
                return record.getLevel() + ": " + record.getMessage() + "\n";
            }
        });

        LOGGER.setUseParentHandlers(false);
        if (DO_LOGGING) {
            handler.setLevel(Level.ALL);
        } else {
            handler.setLevel(Level.OFF);
        }

        LOGGER.addHandler(handler);
    }
}
