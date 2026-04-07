package org.example.cli;

import java.util.function.Function;

/**
 * Parses a raw string to {@link Integer}, wrapping {@link NumberFormatException} in a more
 * descriptive {@link IllegalArgumentException}. Used as {@link ArgsParser#INTEGER}.
 */
class IntegerParser implements Function<String, Integer> {

    @Override
    public Integer apply(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + raw + "' is not a valid integer");
        }
    }
}
