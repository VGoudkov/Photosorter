package com.photosorter.exif;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

/**
 * Reads the EXIF DateTimeOriginal tag from image files using metadata-extractor.
 */
public class ExifDateReader {

    /**
     * Attempts to read the original capture date from the file's EXIF data.
     *
     * @param file path to the image file
     * @return the DateTimeOriginal value if present, empty otherwise
     */
    public Optional<LocalDateTime> readOriginalDate(Path file) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file.toFile());

            // Try SubIFD first (most common location for DateTimeOriginal)
            ExifSubIFDDirectory subIFD = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (subIFD != null) {
                Date dateOriginal = subIFD.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (dateOriginal != null) {
                    return Optional.of(toLocalDateTime(dateOriginal));
                }
            }

            // Fallback to IFD0
            ExifIFD0Directory ifd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (ifd0 != null) {
                Date dateOriginal = ifd0.getDate(ExifIFD0Directory.TAG_DATETIME_ORIGINAL);
                if (dateOriginal != null) {
                    return Optional.of(toLocalDateTime(dateOriginal));
                }
            }

            return Optional.empty();
        } catch (Exception e) {
            // Any error (corrupt file, unsupported format, etc.) → empty
            return Optional.empty();
        }
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
    }
}
