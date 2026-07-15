package com.photosorter.scanner;

import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * Lightweight record representing a discovered photo file with its resolved metadata.
 */
public record PhotoFile(Path path, long size, LocalDateTime resolvedDate) {
}
