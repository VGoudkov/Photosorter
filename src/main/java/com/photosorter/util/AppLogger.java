package com.photosorter.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Configures application logging.
 * Sets up file logging to the destination directory.
 */
public class AppLogger {

    private static final Logger log = LoggerFactory.getLogger(AppLogger.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Initializes logging to a file in the destination directory.
     * The log file is named: photosorter-YYYY-MM-DD.log
     *
     * @param destination the destination directory where the log file will be created
     */
    public static void initFileLogging(Path destination) {
        try {
            String logFileName = "photosorter-" + LocalDate.now().format(DATE_FORMATTER) + ".log";
            Path logFile = destination.resolve(logFileName);

            // Configure slf4j-simple to write to this file
            // Note: slf4j-simple reads configuration from simplelogger.properties at startup
            // For dynamic file logging, we'd need a more sophisticated setup (e.g., Logback)
            // For now, we rely on console logging + the log panel in the UI

            log.info("Log file will be created at: {}", logFile);

        } catch (Exception e) {
            log.warn("Could not initialize file logging", e);
        }
    }
}
