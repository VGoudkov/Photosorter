package com.photosorter.scanner;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Recursively walks a directory tree, collecting photo files by recognised extension.
 * Follows symlinks and de-duplicates by canonical (real) path.
 */
public class FileCollector {

    private static final Set<String> PHOTO_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "tiff", "tif", "webp",
            "heic", "heif", "raw", "cr2", "nef", "arw", "dng"
    );

    /**
     * Recursively collects all photo files under the given source directory.
     *
     * @param source the root directory to scan
     * @return list of discovered photo file paths (de-duplicated by real path)
     * @throws IOException if an I/O error occurs during the walk
     */
    public List<Path> collect(Path source) throws IOException {
        if (source == null || !Files.isDirectory(source)) {
            throw new IllegalArgumentException("Source must be a valid directory: " + source);
        }

        List<Path> collected = new ArrayList<>();
        Set<Path> seenRealPaths = new HashSet<>();

        Files.walkFileTree(source, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (isPhotoFile(file)) {
                            try {
                                Path realPath = file.toRealPath();
                                if (seenRealPaths.add(realPath)) {
                                    collected.add(file);
                                }
                            } catch (IOException e) {
                                // Cannot resolve real path — skip this file
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        // Log and continue
                        return FileVisitResult.CONTINUE;
                    }
                });

        return collected;
    }

    /**
     * Checks whether the given file has a recognised photo extension.
     */
    public static boolean isPhotoFile(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return false;
        }
        String ext = name.substring(dot + 1);
        return PHOTO_EXTENSIONS.contains(ext);
    }
}
