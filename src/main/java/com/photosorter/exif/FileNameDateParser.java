package com.photosorter.exif;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FileNameDateParser {

    private static final String DEFAULT_CONFIG = "/date-patterns.conf";

    private final List<DateTimeFormatter> patterns;

    public FileNameDateParser() {
        this(DEFAULT_CONFIG);
    }

    public FileNameDateParser(String configResource) {
        this.patterns = loadPatterns(configResource);
    }

    public FileNameDateParser(List<DateTimeFormatter> patterns) {
        this.patterns = new ArrayList<>(patterns);
    }

    private static List<DateTimeFormatter> loadPatterns(String resource) {
        List<DateTimeFormatter> result = new ArrayList<>();
        try (InputStream is = FileNameDateParser.class.getResourceAsStream(resource)) {
            if (is == null) {
                return result;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    result.add(DateTimeFormatter.ofPattern(line));
                }
            }
        } catch (IOException e) {
        }
        return result;
    }

    public Optional<LocalDateTime> parseDate(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String baseName = dot > 0 ? name.substring(0, dot) : name;
        return parseDate(baseName);
    }

    public Optional<LocalDateTime> parseDate(String baseName) {
        for (DateTimeFormatter pattern : patterns) {
            try {
                LocalDateTime date = LocalDateTime.parse(baseName, pattern);
                return Optional.of(date);
            } catch (DateTimeParseException e) {
            }
        }
        return Optional.empty();
    }
}
