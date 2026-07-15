package com.photosorter.mover;

import com.photosorter.exif.DateResolver;
import com.photosorter.scanner.FileCollector;
import com.photosorter.scanner.PhotoFile;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Orchestrates the photo sorting process: scan → resolve date → move.
 * Runs on a background thread and dispatches events to the UI thread.
 */
public class SortEngine {

    private static final Logger log = LoggerFactory.getLogger(SortEngine.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final FileCollector fileCollector;
    private final DateResolver dateResolver;
    private final DuplicateChecker duplicateChecker;
    private final ConflictResolver conflictResolver;

    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicInteger scannedCount = new AtomicInteger(0);
    private final AtomicInteger movedCount = new AtomicInteger(0);
    private final AtomicInteger skippedCount = new AtomicInteger(0);
    private final AtomicInteger errorCount = new AtomicInteger(0);

    public SortEngine() {
        this(new FileCollector(), new DateResolver(), new DuplicateChecker(), new ConflictResolver());
    }

    public SortEngine(FileCollector fileCollector, DateResolver dateResolver,
                      DuplicateChecker duplicateChecker, ConflictResolver conflictResolver) {
        this.fileCollector = fileCollector;
        this.dateResolver = dateResolver;
        this.duplicateChecker = duplicateChecker;
        this.conflictResolver = conflictResolver;
    }

    /**
     * Executes the sort operation.
     *
     * @param source      source directory to scan
     * @param destination destination root directory
     * @param listener    callback for progress events
     */
    public void execute(Path source, Path destination, SortListener listener) {
        // Don't reset cancelled here — allow pre-cancellation
        scannedCount.set(0);
        movedCount.set(0);
        skippedCount.set(0);
        errorCount.set(0);
        duplicateChecker.clear();

        try {
            // Validate destination is writable
            if (!Files.isDirectory(destination)) {
                Files.createDirectories(destination);
            }
            if (!Files.isWritable(destination)) {
                throw new IOException("Destination is not writable: " + destination);
            }

            // Pre-scan destination to seed duplicate checker
            preScanDestination(destination);

            // Collect source files
            List<Path> files = fileCollector.collect(source);
            log.info("Found {} photo files to process", files.size());

            // Process each file
            for (Path file : files) {
                if (cancelled.get()) {
                    log.info("Sort cancelled by user");
                    break;
                }

                processFile(file, destination, listener);
            }

            // Complete
            Summary summary = new Summary(
                    scannedCount.get(),
                    movedCount.get(),
                    skippedCount.get(),
                    errorCount.get()
            );
            dispatch(() -> listener.onComplete(summary));

        } catch (Exception e) {
            log.error("Sort failed", e);
            Summary summary = new Summary(
                    scannedCount.get(),
                    movedCount.get(),
                    skippedCount.get(),
                    errorCount.get()
            );
            dispatch(() -> listener.onComplete(summary));
        }
    }

    /**
     * Cancels the sort operation. The engine will stop after the current file.
     */
    public void cancel() {
        cancelled.set(true);
    }

    /**
     * Returns true if cancellation has been requested.
     */
    public boolean isCancelled() {
        return cancelled.get();
    }

    private void processFile(Path file, Path destination, SortListener listener) {
        try {
            // Notify scanned
            dispatch(() -> listener.onScanned(file));
            scannedCount.incrementAndGet();

            // Resolve date
            LocalDateTime date = dateResolver.resolve(file);
            long size = Files.size(file);
            String fileName = file.getFileName().toString();

            // Check duplicate
            if (duplicateChecker.isDuplicate(fileName, size, date)) {
                log.info("Duplicate skipped: {}", fileName);
                skippedCount.incrementAndGet();
                dispatch(() -> listener.onSkipped(file, "Duplicate"));
                dispatchProgress(listener);
                return;
            }

            // Compute target directory
            String datePath = date.format(DATE_FORMATTER);
            Path targetDir = destination.resolve(datePath);
            Files.createDirectories(targetDir);

            // Resolve name conflicts
            Path targetFile = conflictResolver.resolve(targetDir, fileName);

            // Move file
            try {
                Files.move(file, targetFile, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                // Atomic move failed (cross-filesystem), fall back to copy+delete
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                Files.delete(file);
            }

            log.info("Moved: {} → {}", file.getFileName(), targetFile);
            movedCount.incrementAndGet();
            dispatch(() -> listener.onMoved(file, targetFile));
            dispatchProgress(listener);

        } catch (Exception e) {
            log.error("Error processing file: {}", file, e);
            errorCount.incrementAndGet();
            dispatch(() -> listener.onError(file, e));
            dispatchProgress(listener);
        }
    }

    private void preScanDestination(Path destination) throws IOException {
        log.info("Pre-scanning destination for existing files...");
        Files.walkFileTree(destination, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (FileCollector.isPhotoFile(file)) {
                            try {
                                LocalDateTime date = dateResolver.resolve(file);
                                long size = attrs.size();
                                String fileName = file.getFileName().toString();
                                duplicateChecker.mark(fileName, size, date);
                            } catch (Exception e) {
                                log.warn("Could not read metadata for existing file: {}", file);
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }
                });
        log.info("Pre-scan complete. {} known entries in duplicate checker", duplicateChecker.size());
    }

    private void dispatchProgress(SortListener listener) {
        int scanned = scannedCount.get();
        int moved = movedCount.get();
        int skipped = skippedCount.get();
        int errors = errorCount.get();
        dispatch(() -> listener.onProgress(scanned, moved, skipped, errors));
    }

    private void dispatch(Runnable action) {
        try {
            if (Platform.isFxApplicationThread()) {
                action.run();
            } else {
                Platform.runLater(action);
            }
        } catch (IllegalStateException e) {
            // JavaFX toolkit not initialized (e.g., in tests) — run directly
            action.run();
        }
    }
}
