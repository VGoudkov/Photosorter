package com.photosorter.mover;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ConflictResolverTest {

    @TempDir
    Path tempDir;

    private ConflictResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ConflictResolver();
    }

    @Test
    void resolve_returnsOriginalNameWhenNoConflict() {
        Path result = resolver.resolve(tempDir, "photo.jpg");

        assertThat(result.getFileName().toString()).isEqualTo("photo.jpg");
    }

    @Test
    void resolve_appendsSuffixWhenConflictExists() throws IOException {
        Files.createFile(tempDir.resolve("photo.jpg"));

        Path result = resolver.resolve(tempDir, "photo.jpg");

        assertThat(result.getFileName().toString()).isEqualTo("photo (1).jpg");
    }

    @Test
    void resolve_incrementsSuffixForMultipleConflicts() throws IOException {
        Files.createFile(tempDir.resolve("photo.jpg"));
        Files.createFile(tempDir.resolve("photo (1).jpg"));
        Files.createFile(tempDir.resolve("photo (2).jpg"));

        Path result = resolver.resolve(tempDir, "photo.jpg");

        assertThat(result.getFileName().toString()).isEqualTo("photo (3).jpg");
    }

    @Test
    void resolve_handlesFileWithoutExtension() throws IOException {
        Files.createFile(tempDir.resolve("README"));

        Path result = resolver.resolve(tempDir, "README");

        assertThat(result.getFileName().toString()).isEqualTo("README (1)");
    }

    @Test
    void resolve_handlesFileWithMultipleDots() throws IOException {
        Files.createFile(tempDir.resolve("photo.backup.jpg"));

        Path result = resolver.resolve(tempDir, "photo.backup.jpg");

        assertThat(result.getFileName().toString()).isEqualTo("photo.backup (1).jpg");
    }

    @Test
    void resolve_createsPathInTargetDirectory() {
        Path result = resolver.resolve(tempDir, "photo.jpg");

        assertThat(result.getParent()).isEqualTo(tempDir);
    }
}
