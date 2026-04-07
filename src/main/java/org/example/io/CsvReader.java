package org.example.io;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Reads a single column from a classpath CSV resource.
 *
 * <p>The first row is always treated as a header and skipped. Rows where the requested column
 * index is out of range are silently skipped. Results are returned as a lazy {@link Stream} —
 * the underlying {@link java.io.InputStream} stays open until the stream is consumed or closed.
 */
public class CsvReader {

    /**
     * Opens the CSV at {@code resourcePath} (classpath-relative, e.g. {@code "/passwords.csv"}),
     * skips the header row, extracts {@code columnIndex} from each remaining row, applies
     * {@code filter}, and returns the matching values as a stream.
     *
     * @throws IllegalArgumentException if no resource exists at the given path
     */
    public static Stream<String> column(String resourcePath, int columnIndex, Predicate<String> filter) {
        var in = CsvReader.class.getResourceAsStream(resourcePath);
        if (in == null) throw new IllegalArgumentException("Resource not found: " + resourcePath);
        return new BufferedReader(new InputStreamReader(in))
                   .lines()
                   .skip(1) // skip header row
                   .map(line -> line.split(",", -1))
                   .filter(parts -> columnIndex < parts.length)
                   .map(parts -> parts[columnIndex].trim())
                   .filter(filter);
    }
}
