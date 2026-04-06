package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class CsvResource {

    public static Stream<String> column(
        String resourceName,
        int columnIndex,
        Predicate<String> filter
    ) {
        var in = CsvResource.class.getResourceAsStream(resourceName);
        if (in == null) throw new IllegalArgumentException("Resource not found: " + resourceName);

        return new BufferedReader(new InputStreamReader(in))
                   .lines()
                   .skip(1) // skip header
                   .map(line -> line.split(",", -1))
                   .filter(parts -> columnIndex < parts.length)
                   .map(parts -> parts[columnIndex].trim())
                   .filter(filter);
    }
}
