package org.example.cli;

import java.util.function.Function;

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
