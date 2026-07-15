package com.photosorter.testutil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Helper for creating test image files.
 */
public class TestImageHelper {

    /**
     * Creates a minimal valid JPEG file (no EXIF data).
     */
    public static Path createMinimalJpeg(Path dir, String fileName) throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0xFF0000); // Red pixel

        Path file = dir.resolve(fileName);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        Files.write(file, baos.toByteArray());
        return file;
    }

    /**
     * Creates a minimal JPEG file with EXIF DateTimeOriginal tag.
     * The EXIF data is manually constructed to include the specified date.
     *
     * @param dir      directory to create the file in
     * @param fileName file name
     * @param dateTime EXIF DateTimeOriginal value in format "YYYY:MM:DD HH:MM:SS"
     * @return path to the created file
     */
    public static Path createJpegWithExif(Path dir, String fileName, String dateTime) throws IOException {
        // Create a minimal JPEG with EXIF data
        byte[] jpegBytes = buildJpegWithExif(dateTime);
        Path file = dir.resolve(fileName);
        Files.write(file, jpegBytes);
        return file;
    }

    /**
     * Builds a minimal JPEG file with EXIF DateTimeOriginal tag.
     */
    private static byte[] buildJpegWithExif(String dateTime) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            // SOI (Start of Image)
            baos.write(new byte[]{(byte) 0xFF, (byte) 0xD8});

            // APP1 marker (EXIF)
            baos.write(new byte[]{(byte) 0xFF, (byte) 0xE1});

            // Build EXIF data
            byte[] exifData = buildExifData(dateTime);

            // Length (2 bytes, includes the 2 length bytes themselves)
            int length = exifData.length + 2;
            baos.write((length >> 8) & 0xFF);
            baos.write(length & 0xFF);

            // EXIF data
            baos.write(exifData);

            // Minimal image data (SOI + EOI)
            // Add a minimal JPEG image (1x1 white pixel)
            byte[] minimalImage = {
                    (byte) 0xFF, (byte) 0xC0, 0x00, 0x0B, 0x08, 0x00, 0x01, 0x00, 0x01, 0x01, 0x01, 0x11, 0x00,
                    (byte) 0xFF, (byte) 0xC4, 0x00, 0x1F, 0x00, 0x00, 0x01, 0x05, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06,
                    0x07, 0x08, 0x09, 0x0A, 0x0B,
                    (byte) 0xFF, (byte) 0xC4, 0x00, (byte) 0xB5, 0x10, 0x00, 0x02, 0x01, 0x03, 0x03, 0x02, 0x04,
                    0x03, 0x05, 0x05, 0x04, 0x04, 0x00, 0x00, 0x01, 0x7D, 0x01, 0x02, 0x03, 0x00, 0x04, 0x11,
                    0x05, 0x12, 0x21, 0x31, 0x41, 0x06, 0x13, 0x51, 0x61, 0x07, 0x22, 0x71, 0x14, 0x32, (byte) 0x81,
                    (byte) 0x91, (byte) 0xA1, 0x08, 0x23, 0x42, (byte) 0xB1, (byte) 0xC1, 0x15, 0x52, (byte) 0xD1, (byte) 0xF0, 0x24, 0x33,
                    0x62, 0x72, (byte) 0x82, 0x09, 0x0A, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x25, 0x26, 0x27, 0x28, 0x29,
                    0x2A, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A,
                    0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5A, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x6A,
                    0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7A, (byte) 0x83, (byte) 0x84, (byte) 0x85, (byte) 0x86, (byte) 0x87,
                    (byte) 0x88, (byte) 0x89, (byte) 0x8A, (byte) 0x92, (byte) 0x93, (byte) 0x94, (byte) 0x95, (byte) 0x96, (byte) 0x97, (byte) 0x98, (byte) 0x99,
                    (byte) 0x9A, (byte) 0xA2, (byte) 0xA3, (byte) 0xA4, (byte) 0xA5, (byte) 0xA6, (byte) 0xA7, (byte) 0xA8, (byte) 0xA9, (byte) 0xAA,
                    (byte) 0xB2, (byte) 0xB3, (byte) 0xB4, (byte) 0xB5, (byte) 0xB6, (byte) 0xB7, (byte) 0xB8, (byte) 0xB9, (byte) 0xBA, (byte) 0xC2,
                    (byte) 0xC3, (byte) 0xC4, (byte) 0xC5, (byte) 0xC6, (byte) 0xC7, (byte) 0xC8, (byte) 0xC9, (byte) 0xCA, (byte) 0xD2, (byte) 0xD3,
                    (byte) 0xD4, (byte) 0xD5, (byte) 0xD6, (byte) 0xD7, (byte) 0xD8, (byte) 0xD9, (byte) 0xDA, (byte) 0xE1, (byte) 0xE2, (byte) 0xE3,
                    (byte) 0xE4, (byte) 0xE5, (byte) 0xE6, (byte) 0xE7, (byte) 0xE8, (byte) 0xE9, (byte) 0xEA, (byte) 0xF1, (byte) 0xF2, (byte) 0xF3,
                    (byte) 0xF4, (byte) 0xF5, (byte) 0xF6, (byte) 0xF7, (byte) 0xF8, (byte) 0xF9, (byte) 0xFA,
                    (byte) 0xFF, (byte) 0xDA, 0x00, 0x08, 0x01, 0x01, 0x00, 0x00, 0x3F, 0x00, 0x7B, (byte) 0x94, 0x11, 0x00,
                    (byte) 0xFE, (byte) 0x8A, 0x28, 0x02, (byte) 0xA9, (byte) 0xFF, (byte) 0xD9
            };
            baos.write(minimalImage);

        } catch (IOException e) {
            throw new RuntimeException("Failed to build JPEG with EXIF", e);
        }

        return baos.toByteArray();
    }

    /**
     * Builds EXIF data with DateTimeOriginal tag.
     */
    private static byte[] buildExifData(String dateTime) {
        ByteArrayOutputStream exif = new ByteArrayOutputStream();

        try {
            // EXIF header: "Exif\0\0"
            exif.write("Exif\0\0".getBytes());

            // TIFF header (little-endian)
            exif.write(new byte[]{'I', 'I'}); // Little-endian byte order
            exif.write(new byte[]{0x2A, 0x00}); // TIFF magic number
            exif.write(new byte[]{0x08, 0x00, 0x00, 0x00}); // Offset to first IFD (8 bytes from start of TIFF header)

            // IFD0 (Image File Directory)
            int numEntries = 1; // Just DateTimeOriginal
            exif.write(numEntries & 0xFF);
            exif.write((numEntries >> 8) & 0xFF);

            // IFD Entry: DateTimeOriginal (tag 0x9003)
            exif.write(new byte[]{0x03, (byte) 0x90}); // Tag: DateTimeOriginal (0x9003) in little-endian
            exif.write(new byte[]{0x02, 0x00}); // Type: ASCII (2)
            exif.write(new byte[]{0x14, 0x00, 0x00, 0x00}); // Count: 20 bytes (19 chars + null)
            exif.write(new byte[]{0x1A, 0x00, 0x00, 0x00}); // Value offset: 26 bytes from TIFF header start

            // Next IFD offset (0 = no more IFDs)
            exif.write(new byte[]{0x00, 0x00, 0x00, 0x00});

            // DateTimeOriginal value (20 bytes: "YYYY:MM:DD HH:MM:SS\0")
            byte[] dateBytes = dateTime.getBytes();
            exif.write(dateBytes);
            exif.write(0x00); // Null terminator

            // Pad to 20 bytes if needed
            int padding = 20 - dateBytes.length - 1;
            for (int i = 0; i < padding; i++) {
                exif.write(0x00);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to build EXIF data", e);
        }

        return exif.toByteArray();
    }
}
