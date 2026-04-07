package org.example.io;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class TestCsvReader {

    @Nested
    class ColumnReading {

        @Test
        void readsFirstColumn() {
            List<String> result = CsvReader.column("/test.csv", 0, s -> true).toList();

            assertThat(result).containsExactly("alice", "bob", "carol");
        }

        @Test
        void readsSecondColumn() {
            List<String> result = CsvReader.column("/test.csv", 1, s -> true).toList();

            assertThat(result).containsExactly("42", "17", "99");
        }

        @Test
        void readsThirdColumn() {
            List<String> result = CsvReader.column("/test.csv", 2, s -> true).toList();

            assertThat(result).containsExactly("alpha", "beta", "gamma");
        }

        @Test
        void skipsHeaderRow() {
            List<String> result = CsvReader.column("/test.csv", 0, s -> true).toList();

            assertThat(result).doesNotContain("name");
        }
    }

    @Nested
    class Filtering {

        @Test
        void appliesFilter() {
            List<String> result = CsvReader.column("/test.csv", 0, s -> s.startsWith("a")).toList();

            assertThat(result).containsExactly("alice");
        }

        @Test
        void filterThatExcludesEverything_returnsEmptyStream() {
            List<String> result = CsvReader.column("/test.csv", 0, s -> false).toList();

            assertThat(result).isEmpty();
        }

        @Test
        void filterOnNumericColumn() {
            List<String> result = CsvReader.column("/test.csv", 1, s -> Integer.parseInt(s) > 20).toList();

            assertThat(result).containsExactly("42", "99");
        }
    }

    @Nested
    class CornerCases {

        // Opinion: the parser silently skips rows that are too short — worth pinning this
        // behaviour so a future refactor doesn't accidentally blow up on sparse CSV files.
        @Test
        void columnIndexBeyondRowWidth_rowIsSilentlySkipped() {
            // column index 99 doesn't exist on any row in test.csv
            List<String> result = CsvReader.column("/test.csv", 99, s -> true).toList();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class Failures {

        @Test
        void throwsForMissingResource() {
            assertThatThrownBy(() -> CsvReader.column("/nonexistent.csv", 0, s -> true).toList())
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
