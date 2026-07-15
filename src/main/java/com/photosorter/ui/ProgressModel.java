package com.photosorter.ui;

import javafx.beans.property.SimpleIntegerProperty;

/**
 * Observable model for sort progress counters.
 * Properties are bound to UI labels and updated from the sort engine.
 */
public class ProgressModel {

    private final SimpleIntegerProperty scanned = new SimpleIntegerProperty(0);
    private final SimpleIntegerProperty moved = new SimpleIntegerProperty(0);
    private final SimpleIntegerProperty skipped = new SimpleIntegerProperty(0);
    private final SimpleIntegerProperty errors = new SimpleIntegerProperty(0);

    public SimpleIntegerProperty scannedProperty() { return scanned; }
    public SimpleIntegerProperty movedProperty() { return moved; }
    public SimpleIntegerProperty skippedProperty() { return skipped; }
    public SimpleIntegerProperty errorsProperty() { return errors; }

    public int getScanned() { return scanned.get(); }
    public int getMoved() { return moved.get(); }
    public int getSkipped() { return skipped.get(); }
    public int getErrors() { return errors.get(); }

    public void update(int scanned, int moved, int skipped, int errors) {
        this.scanned.set(scanned);
        this.moved.set(moved);
        this.skipped.set(skipped);
        this.errors.set(errors);
    }

    public void reset() {
        scanned.set(0);
        moved.set(0);
        skipped.set(0);
        errors.set(0);
    }
}
