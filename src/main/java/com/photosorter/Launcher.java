package com.photosorter;

/**
 * Separate launcher class that does NOT extend javafx.application.Application.
 * <p>
 * JavaFX 11+ forbids the class that calls {@code Application.launch()} from
 * extending {@code Application} when running on the classpath (non-modular).
 * This thin wrapper delegates to {@link App#main(String[])} so the JavaFX
 * runtime can initialise correctly.
 */
public class Launcher {

    public static void main(String[] args) {
        App.main(args);
    }
}
