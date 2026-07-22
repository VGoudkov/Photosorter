package com.photosorter.exif;

import com.photosorter.testutil.TestImageHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DateResolverTest {

    @TempDir
    Path tempDir;

    private DateResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new DateResolver();
    }

    @Test
    void resolve_fallsBackToLastModifiedWhenNoExif() throws IOException {
        Path jpeg = TestImageHelper.createMinimalJpeg(tempDir, "noexif.jpg");

        // Set a known last-modified time
        LocalDateTime expectedDate = LocalDateTime.of(2020, 7, 15, 14, 30, 0);
        FileTime fileTime = FileTime.from(expectedDate.atZone(ZoneId.systemDefault()).toInstant());
        Files.setLastModifiedTime(jpeg, fileTime);

        LocalDateTime result = resolver.resolve(jpeg);

        assertThat(result).isEqualTo(expectedDate);
    }

    @Test
    void resolve_returnsDateForValidImage() throws IOException {
        Path jpeg = TestImageHelper.createMinimalJpeg(tempDir, "photo.jpg");

        LocalDateTime result = resolver.resolve(jpeg);

        // Should return some date (either EXIF or fallback)
        assertThat(result).isNotNull();
    }

    @Test
    void resolve_usesFileNameDateBeforeLastModified() throws IOException {
        FileNameDateParser parser = new FileNameDateParser(
                List.of(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        DateResolver customResolver = new DateResolver(new ExifDateReader(), parser);

        Path file = TestImageHelper.createMinimalJpeg(tempDir, "20210620_153000.jpg");
        LocalDateTime expectedDate = LocalDateTime.of(2021, 6, 20, 15, 30, 0);
        // Set a different last-modified to verify it's not used
        LocalDateTime differentDate = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
        FileTime fileTime = FileTime.from(differentDate.atZone(ZoneId.systemDefault()).toInstant());
        Files.setLastModifiedTime(file, fileTime);

        LocalDateTime result = customResolver.resolve(file);

        assertThat(result).isEqualTo(expectedDate);
    }

    @Test
    void resolve_stillFallsBackToLastModifiedWhenFileNameDoesNotMatch() throws IOException {
        Path jpeg = TestImageHelper.createMinimalJpeg(tempDir, "noexif.jpg");

        LocalDateTime expectedDate = LocalDateTime.of(2020, 7, 15, 14, 30, 0);
        FileTime fileTime = FileTime.from(expectedDate.atZone(ZoneId.systemDefault()).toInstant());
        Files.setLastModifiedTime(jpeg, fileTime);

        LocalDateTime result = resolver.resolve(jpeg);

        assertThat(result).isEqualTo(expectedDate);
    }
}
