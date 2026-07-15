package com.photosorter.mover;

import com.photosorter.testutil.TestImageHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SortEngineTest {

    @TempDir
    Path sourceDir;

    @TempDir
    Path destDir;

    private SortEngine engine;
    private TestSortListener listener;

    @BeforeEach
    void setUp() {
        engine = new SortEngine();
        listener = new TestSortListener();
    }

    @Test
    void execute_movesFilesToCorrectDateFolder() throws IOException {
        // Create test images with known last-modified dates
        Path photo1 = TestImageHelper.createMinimalJpeg(sourceDir, "photo1.jpg");
        Path photo2 = TestImageHelper.createMinimalJpeg(sourceDir, "photo2.jpg");

        // Set specific last-modified times
        LocalDateTime date1 = LocalDateTime.of(2020, 7, 15, 14, 30, 0);
        LocalDateTime date2 = LocalDateTime.of(2021, 12, 25, 10, 0, 0);

        Files.setLastModifiedTime(photo1,
                java.nio.file.attribute.FileTime.from(date1.atZone(ZoneId.systemDefault()).toInstant()));
        Files.setLastModifiedTime(photo2,
                java.nio.file.attribute.FileTime.from(date2.atZone(ZoneId.systemDefault()).toInstant()));

        engine.execute(sourceDir, destDir, listener);

        // Verify files were moved to correct date folders
        Path expectedDir1 = destDir.resolve("2020/07/15");
        Path expectedDir2 = destDir.resolve("2021/12/25");

        assertThat(Files.exists(expectedDir1.resolve("photo1.jpg"))).isTrue();
        assertThat(Files.exists(expectedDir2.resolve("photo2.jpg"))).isTrue();

        // Verify summary
        assertThat(listener.summary).isNotNull();
        assertThat(listener.summary.scanned()).isEqualTo(2);
        assertThat(listener.summary.moved()).isEqualTo(2);
        assertThat(listener.summary.skipped()).isZero();
        assertThat(listener.summary.errors()).isZero();
    }

    @Test
    void execute_skipsDuplicates() throws IOException {
        // Create a file in destination that matches a source file
        Path sourcePhoto = TestImageHelper.createMinimalJpeg(sourceDir, "photo.jpg");
        LocalDateTime date = LocalDateTime.of(2020, 7, 15, 14, 30, 0);
        Files.setLastModifiedTime(sourcePhoto,
                java.nio.file.attribute.FileTime.from(date.atZone(ZoneId.systemDefault()).toInstant()));

        // Create matching file in destination
        Path destDateDir = Files.createDirectories(destDir.resolve("2020/07/15"));
        Path destPhoto = TestImageHelper.createMinimalJpeg(destDateDir, "photo.jpg");
        Files.setLastModifiedTime(destPhoto,
                java.nio.file.attribute.FileTime.from(date.atZone(ZoneId.systemDefault()).toInstant()));

        engine.execute(sourceDir, destDir, listener);

        // Source file should still exist (not moved)
        assertThat(Files.exists(sourcePhoto)).isTrue();

        // Verify summary
        assertThat(listener.summary.scanned()).isEqualTo(1);
        assertThat(listener.summary.moved()).isZero();
        assertThat(listener.summary.skipped()).isEqualTo(1);
    }

    @Test
    void execute_handlesNameCollisions() throws IOException {
        // Create source file
        Path sourcePhoto = TestImageHelper.createMinimalJpeg(sourceDir, "photo.jpg");
        LocalDateTime date = LocalDateTime.of(2020, 7, 15, 14, 30, 0);
        Files.setLastModifiedTime(sourcePhoto,
                java.nio.file.attribute.FileTime.from(date.atZone(ZoneId.systemDefault()).toInstant()));

        // Create a file with same name but different content/size in destination
        Path destDateDir = Files.createDirectories(destDir.resolve("2020/07/15"));
        Files.writeString(destDateDir.resolve("photo.jpg"), "different content");

        engine.execute(sourceDir, destDir, listener);

        // Source file should be moved with a suffix
        assertThat(Files.exists(destDateDir.resolve("photo (1).jpg"))).isTrue();

        // Verify summary
        assertThat(listener.summary.moved()).isEqualTo(1);
    }

    @Test
    void execute_handlesEmptySource() throws IOException {
        engine.execute(sourceDir, destDir, listener);

        assertThat(listener.summary.scanned()).isZero();
        assertThat(listener.summary.moved()).isZero();
    }

    @Test
    void cancel_setsCancelledFlag() {
        // Verify that cancel() sets the cancelled flag
        assertThat(engine.isCancelled()).isFalse();
        
        engine.cancel();
        
        assertThat(engine.isCancelled()).isTrue();
    }

    /**
     * Test listener that captures events for verification.
     */
    private static class TestSortListener implements SortListener {
        final List<Path> scannedFiles = new ArrayList<>();
        final List<String> skippedReasons = new ArrayList<>();
        final List<Exception> errors = new ArrayList<>();
        Summary summary;

        @Override
        public void onScanned(Path file) {
            scannedFiles.add(file);
        }

        @Override
        public void onMoved(Path from, Path to) {
            // Track if needed
        }

        @Override
        public void onSkipped(Path file, String reason) {
            skippedReasons.add(reason);
        }

        @Override
        public void onError(Path file, Exception ex) {
            errors.add(ex);
        }

        @Override
        public void onProgress(int scanned, int moved, int skipped, int errors) {
            // Track if needed
        }

        @Override
        public void onComplete(Summary summary) {
            this.summary = summary;
        }
    }
}
