package org.example.io;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class CsvReader {

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
