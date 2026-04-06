package org.example.cli;

import java.util.function.Function;

record ArgDef<T>(String name, Class<T> type, Function<String, T> parse, boolean required, T defaultValue) {

    Object applyParse(String raw) {
        return parse.apply(raw);
    }
}
