package com.photosorter.mover;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicateCheckerTest {

    private DuplicateChecker checker;

    @BeforeEach
    void setUp() {
        checker = new DuplicateChecker();
    }

    @Test
    void isDuplicate_returnsFalseForNewEntry() {
        LocalDateTime date = LocalDateTime.of(2020, 7, 15, 14, 30, 0);

        boolean result = checker.isDuplicate("photo.jpg", 12345, date);

        assertThat(result).isFalse();
    }

    @Test
    void isDuplicate_returnsTrueForExactMatch() {
        LocalDateTime date = LocalDateTime.of(2020, 7, 15, 14, 30, 0);

        checker.isDuplicate("photo.jpg", 12345, date); // First call registers it
        boolean result = checker.isDuplicate("photo.jpg", 12345, date); // Second call detects duplicate

        assertThat(result).isTrue();
    }

    @Test
    void isDuplicate_returnsFalseForDifferentName() {
        LocalDateTime date = LocalDateTime.of(2020, 7, 15, 14, 30, 0);

        checker.isDuplicate("photo1.jpg", 12345, date);
        boolean result = checker.isDuplicate("photo2.jpg", 12345, date);

        assertThat(result).isFalse();
    }

    @Test
    void isDuplicate_returnsFalseForDifferentSize() {
        LocalDateTime date = LocalDateTime.of(2020, 7, 15, 14, 30, 0);

        checker.isDuplicate("photo.jpg", 12345, date);
        boolean result = checker.isDuplicate("photo.jpg", 12346, date);

        assertThat(result).isFalse();
    }

    @Test
    void isDuplicate_returnsFalseForDifferentDate() {
        LocalDateTime date1 = LocalDateTime.of(2020, 7, 15, 14, 30, 0);
        LocalDateTime date2 = LocalDateTime.of(2020, 7, 15, 14, 30, 1);

        checker.isDuplicate("photo.jpg", 12345, date1);
        boolean result = checker.isDuplicate("photo.jpg", 12345, date2);

        assertThat(result).isFalse();
    }

    @Test
    void isDuplicate_truncatesToSecondPrecision() {
        // Dates with sub-second differences should be considered the same
        LocalDateTime date1 = LocalDateTime.of(2020, 7, 15, 14, 30, 0, 123456789);
        LocalDateTime date2 = LocalDateTime.of(2020, 7, 15, 14, 30, 0, 987654321);

        checker.isDuplicate("photo.jpg", 12345, date1);
        boolean result = checker.isDuplicate("photo.jpg", 12345, date2);

        assertThat(result).isTrue();
    }

    @Test
    void mark_registersEntryWithoutChecking() {
        LocalDateTime date = LocalDateTime.of(2020, 7, 15, 14, 30, 0);

        checker.mark("photo.jpg", 12345, date);

        // Now isDuplicate should return true
        boolean result = checker.isDuplicate("photo.jpg", 12345, date);
        assertThat(result).isTrue();
    }

    @Test
    void size_returnsNumberOfEntries() {
        LocalDateTime date = LocalDateTime.of(2020, 7, 15, 14, 30, 0);

        assertThat(checker.size()).isZero();

        checker.mark("photo1.jpg", 12345, date);
        assertThat(checker.size()).isEqualTo(1);

        checker.mark("photo2.jpg", 67890, date);
        assertThat(checker.size()).isEqualTo(2);
    }

    @Test
    void clear_removesAllEntries() {
        LocalDateTime date = LocalDateTime.of(2020, 7, 15, 14, 30, 0);

        checker.mark("photo.jpg", 12345, date);
        assertThat(checker.size()).isEqualTo(1);

        checker.clear();
        assertThat(checker.size()).isZero();

        // Should no longer detect as duplicate
        boolean result = checker.isDuplicate("photo.jpg", 12345, date);
        assertThat(result).isFalse();
    }
}
