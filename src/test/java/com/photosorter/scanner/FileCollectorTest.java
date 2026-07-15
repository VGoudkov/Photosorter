package com.photosorter.scanner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileCollectorTest {

    @TempDir
    Path tempDir;

    private FileCollector collector;

    @BeforeEach
    void setUp() {
        collector = new FileCollector();
    }

    @Test
    void collect_findsJpegFiles() throws IOException {
        Files.createFile(tempDir.resolve("photo1.jpg"));
        Files.createFile(tempDir.resolve("photo2.jpeg"));
        Files.createFile(tempDir.resolve("document.txt"));

        List<Path> result = collector.collect(tempDir);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(p -> p.getFileName().toString())
                .containsExactlyInAnyOrder("photo1.jpg", "photo2.jpeg");
    }

    @Test
    void collect_findsAllRecognisedExtensions() throws IOException {
        String[] extensions = {"jpg", "jpeg", "png", "tiff", "tif", "webp",
                "heic", "heif", "raw", "cr2", "nef", "arw", "dng"};

        for (String ext : extensions) {
            Files.createFile(tempDir.resolve("photo." + ext));
        }
        Files.createFile(tempDir.resolve("readme.md"));
        Files.createFile(tempDir.resolve("video.mp4"));

        List<Path> result = collector.collect(tempDir);

        assertThat(result).hasSize(extensions.length);
    }

    @Test
    void collect_isCaseInsensitive() throws IOException {
        Files.createFile(tempDir.resolve("photo.JPG"));
        Files.createFile(tempDir.resolve("photo.Jpeg"));
        Files.createFile(tempDir.resolve("photo.PNG"));

        List<Path> result = collector.collect(tempDir);

        assertThat(result).hasSize(3);
    }

    @Test
    void collect_recursesSubdirectories() throws IOException {
        Files.createFile(tempDir.resolve("root.jpg"));
        Path subDir = Files.createDirectory(tempDir.resolve("subdir"));
        Files.createFile(subDir.resolve("nested.jpg"));
        Path deepDir = Files.createDirectory(subDir.resolve("deep"));
        Files.createFile(deepDir.resolve("deep.png"));

        List<Path> result = collector.collect(tempDir);

        assertThat(result).hasSize(3);
        assertThat(result).extracting(p -> p.getFileName().toString())
                .containsExactlyInAnyOrder("root.jpg", "nested.jpg", "deep.png");
    }

    @Test
    void collect_deduplicatesByRealPath() throws IOException {
        Path realFile = Files.createFile(tempDir.resolve("real.jpg"));
        Path symlink = tempDir.resolve("link.jpg");

        try {
            Files.createSymbolicLink(symlink, realFile);
        } catch (IOException e) {
            // Symlinks not supported on this platform — skip
            return;
        }

        List<Path> result = collector.collect(tempDir);

        assertThat(result).hasSize(1);
    }

    @Test
    void collect_returnsEmptyForEmptyDirectory() throws IOException {
        List<Path> result = collector.collect(tempDir);

        assertThat(result).isEmpty();
    }

    @Test
    void collect_throwsForNullSource() {
        assertThatThrownBy(() -> collector.collect(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void collect_throwsForNonExistentSource() {
        Path nonExistent = tempDir.resolve("nonexistent");

        assertThatThrownBy(() -> collector.collect(nonExistent))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isPhotoFile_recognisesValidExtensions() {
        assertThat(FileCollector.isPhotoFile(Path.of("photo.jpg"))).isTrue();
        assertThat(FileCollector.isPhotoFile(Path.of("photo.JPEG"))).isTrue();
        assertThat(FileCollector.isPhotoFile(Path.of("photo.png"))).isTrue();
        assertThat(FileCollector.isPhotoFile(Path.of("photo.heic"))).isTrue();
    }

    @Test
    void isPhotoFile_rejectsInvalidExtensions() {
        assertThat(FileCollector.isPhotoFile(Path.of("document.txt"))).isFalse();
        assertThat(FileCollector.isPhotoFile(Path.of("video.mp4"))).isFalse();
        assertThat(FileCollector.isPhotoFile(Path.of("noextension"))).isFalse();
        assertThat(FileCollector.isPhotoFile(Path.of("file."))).isFalse();
    }
}
