package com.photosorter.exif;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FileNameDateParserTest {

    @Test
    void parseDate_returnsDateForYyyyMMdd_HHmmss() {
        FileNameDateParser parser = new FileNameDateParser(
                List.of(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        Optional<LocalDateTime> result = parser.parseDate(Path.of("20160818_124425.jpg"));
        assertThat(result).hasValue(LocalDateTime.of(2016, 8, 18, 12, 44, 25));
    }

    @Test
    void parseDate_returnsDateForYyyyMMdd_HHmmssWithoutExtension() {
        FileNameDateParser parser = new FileNameDateParser(
                List.of(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        Optional<LocalDateTime> result = parser.parseDate(Path.of("20160818_124425"));
        assertThat(result).hasValue(LocalDateTime.of(2016, 8, 18, 12, 44, 25));
    }

    @Test
    void parseDate_returnsDateForYyyy_MM_dd_HHmmss() {
        FileNameDateParser parser = new FileNameDateParser(
                List.of(DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss")));
        Optional<LocalDateTime> result = parser.parseDate(Path.of("2015-08-22 21.59.57.jpg"));
        assertThat(result).hasValue(LocalDateTime.of(2015, 8, 22, 21, 59, 57));
    }

    @Test
    void parseDate_returnsDateForMMddYyyyHHmm() {
        FileNameDateParser parser = new FileNameDateParser(
                List.of(DateTimeFormatter.ofPattern("MM dd yyyy HH mm")));
        Optional<LocalDateTime> result = parser.parseDate(Path.of("07 15 2020 14 30.jpg"));
        assertThat(result).hasValue(LocalDateTime.of(2020, 7, 15, 14, 30, 0));
    }

    @Test
    void parseDate_returnsEmptyForUnmatchedPattern() {
        FileNameDateParser parser = new FileNameDateParser(
                List.of(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        Optional<LocalDateTime> result = parser.parseDate(Path.of("random_name.jpg"));
        assertThat(result).isEmpty();
    }

    @Test
    void parseDate_triesPatternsInOrderAndReturnsFirstMatch() {
        FileNameDateParser parser = new FileNameDateParser(List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss"),
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        Optional<LocalDateTime> result = parser.parseDate(Path.of("20160818_124425.jpg"));
        assertThat(result).hasValue(LocalDateTime.of(2016, 8, 18, 12, 44, 25));
    }

    @Test
    void parseDate_skipsToNextPatternWhenFirstFails() {
        FileNameDateParser parser = new FileNameDateParser(List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss"),
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        Optional<LocalDateTime> result = parser.parseDate(Path.of("2015-08-22 21.59.57.jpg"));
        assertThat(result).hasValue(LocalDateTime.of(2015, 8, 22, 21, 59, 57));
    }

    @Test
    void parseDate_returnsEmptyForEmptyPatternList() {
        FileNameDateParser parser = new FileNameDateParser(List.of());
        Optional<LocalDateTime> result = parser.parseDate(Path.of("20160818_124425.jpg"));
        assertThat(result).isEmpty();
    }
}
