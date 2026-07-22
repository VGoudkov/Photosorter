package com.photosorter.exif;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Resolves the "date taken" for a photo file.
 * Primary: EXIF DateTimeOriginal.
 * Secondary: file name pattern matching.
 * Fallback: file system last-modified timestamp.
 */
public class DateResolver {

    private final ExifDateReader exifReader;
    private final FileNameDateParser fileNameParser;

    public DateResolver() {
        this(new ExifDateReader(), new FileNameDateParser());
    }

    public DateResolver(ExifDateReader exifReader, FileNameDateParser fileNameParser) {
        this.exifReader = exifReader;
        this.fileNameParser = fileNameParser;
    }

    /**
     * Resolves the date for the given photo file.
     *
     * @param file path to the image file
     * @return the resolved date/time (never null)
     * @throws IOException if the file cannot be read for fallback date
     */
    public LocalDateTime resolve(Path file) throws IOException {
        return exifReader.readOriginalDate(file)
                .or(() -> fileNameParser.parseDate(file))
                .orElseGet(() -> getLastModified(file));
    }

    private LocalDateTime getLastModified(Path file) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
            return LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault());
        } catch (IOException e) {
            // Last resort: epoch
            return LocalDateTime.of(1970, 1, 1, 0, 0, 0);
        }
    }
}
