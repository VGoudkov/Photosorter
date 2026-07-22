package com.photosorter.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {

    private static final Path CONFIG_PATH = Path.of(System.getProperty("user.dir"), "config.properties");

    public static String get(String key) {
        return readAll().get(key);
    }

    public static void set(String key, String value) {
        Map<String, String> props = readAll();
        if (value != null) {
            props.put(key, value);
        } else {
            props.remove(key);
        }
        writeAll(props);
    }

    private static Map<String, String> readAll() {
        Map<String, String> result = new HashMap<>();
        try {
            List<String> lines = Files.readAllLines(CONFIG_PATH, StandardCharsets.UTF_8);
            for (String line : lines) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq < 0) {
                    continue;
                }
                String k = line.substring(0, eq).strip();
                String v = line.substring(eq + 1).strip();
                result.put(k, v);
            }
        } catch (IOException e) {
            // file does not exist yet
        }
        return result;
    }

    private static void writeAll(Map<String, String> props) {
        try {
            var lines = props.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .toList();
            Files.write(CONFIG_PATH, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            // silently ignore
        }
    }
}
