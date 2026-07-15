package com.photosorter.mover;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

/**
 * Detects duplicate photos based on the triple: (file name, file size, shooting date).
 * All three must match for a file to be considered a duplicate.
 * Date comparison is at second-level precision.
 */
public class DuplicateChecker {

    private final Set<String> knownEntries = new HashSet<>();

    /**
     * Checks whether a photo with the given attributes is already known.
     * If not known, it is registered.
     *
     * @param fileName the file name (base + extension)
     * @param size     file size in bytes
     * @param date     the resolved shooting date
     * @return true if this is a duplicate (already seen), false otherwise
     */
    public boolean isDuplicate(String fileName, long size, LocalDateTime date) {
        String key = buildKey(fileName, size, date);
        return !knownEntries.add(key);
    }

    /**
     * Registers a photo as known without checking for duplicates.
     */
    public void mark(String fileName, long size, LocalDateTime date) {
        knownEntries.add(buildKey(fileName, size, date));
    }

    /**
     * Returns the number of known entries.
     */
    public int size() {
        return knownEntries.size();
    }

    /**
     * Clears all known entries.
     */
    public void clear() {
        knownEntries.clear();
    }

    private String buildKey(String fileName, long size, LocalDateTime date) {
        LocalDateTime truncated = date.truncatedTo(ChronoUnit.SECONDS);
        return fileName + "|" + size + "|" + truncated;
    }
}
