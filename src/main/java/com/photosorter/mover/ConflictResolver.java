package com.photosorter.mover;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves file name collisions at the destination by appending a numeric suffix.
 * Example: "photo.jpg" → "photo (1).jpg" → "photo (2).jpg" etc.
 */
public class ConflictResolver {

    /**
     * Returns a target path that does not currently exist on disk.
     * If the proposed target already exists, a numeric suffix is appended
     * before the extension until a free name is found.
     *
     * @param targetDir  the destination directory
     * @param fileName   the original file name (e.g. "photo.jpg")
     * @return a Path within targetDir that does not yet exist
     */
    public Path resolve(Path targetDir, String fileName) {
        Path candidate = targetDir.resolve(fileName);
        if (!Files.exists(candidate)) {
            return candidate;
        }

        String baseName = getBaseName(fileName);
        String extension = getExtension(fileName);

        int counter = 1;
        while (true) {
            String newName = baseName + " (" + counter + ")" + extension;
            candidate = targetDir.resolve(newName);
            if (!Files.exists(candidate)) {
                return candidate;
            }
            counter++;
        }
    }

    private String getBaseName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(dot) : "";
    }
}
