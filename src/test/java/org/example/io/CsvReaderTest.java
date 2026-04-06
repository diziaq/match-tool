package org.example.io;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvReaderTest {

    @Test
    void readsFirstColumn() {
        List<String> result = CsvReader.column("/test.csv", 0, s -> true).toList();

        assertEquals(List.of("alice", "bob", "carol"), result);
    }

    @Test
    void readsSecondColumn() {
        List<String> result = CsvReader.column("/test.csv", 1, s -> true).toList();

        assertEquals(List.of("42", "17", "99"), result);
    }

    @Test
    void readsThirdColumn() {
        List<String> result = CsvReader.column("/test.csv", 2, s -> true).toList();

        assertEquals(List.of("alpha", "beta", "gamma"), result);
    }

    @Test
    void skipsHeaderRow() {
        List<String> result = CsvReader.column("/test.csv", 0, s -> true).toList();

        assertFalse(result.contains("name"));
    }

    @Test
    void appliesFilter() {
        List<String> result = CsvReader.column("/test.csv", 0, s -> s.startsWith("a")).toList();

        assertEquals(List.of("alice"), result);
    }

    @Test
    void filterThatExcludesEverything_returnsEmptyStream() {
        List<String> result = CsvReader.column("/test.csv", 0, s -> false).toList();

        assertTrue(result.isEmpty());
    }

    @Test
    void throwsForMissingResource() {
        assertThrows(IllegalArgumentException.class,
            () -> CsvReader.column("/nonexistent.csv", 0, s -> true).toList());
    }

    @Test
    void filterOnNumericColumn() {
        List<String> result = CsvReader.column("/test.csv", 1, s -> Integer.parseInt(s) > 20).toList();

        assertEquals(List.of("42", "99"), result);
    }
}
