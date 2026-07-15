package com.photosorter.mover;

import java.nio.file.Path;

/**
 * Callback interface for receiving sort progress events.
 * All methods are called on the JavaFX Application Thread.
 */
public interface SortListener {

    void onScanned(Path file);

    void onMoved(Path from, Path to);

    void onSkipped(Path file, String reason);

    void onError(Path file, Exception ex);

    void onProgress(int scanned, int moved, int skipped, int errors);

    void onComplete(Summary summary);
}
