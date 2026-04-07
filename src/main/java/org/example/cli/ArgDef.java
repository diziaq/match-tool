package org.example.cli;

import java.util.function.Function;

/**
 * Descriptor for a single registered CLI argument.
 *
 * <p>Instances are created by {@link ArgSchema.Builder#required} and
 * {@link ArgSchema.Builder#optional}, or by {@link ArgSchema#fromText}. The {@code parse}
 * function converts the raw string value to the typed result; throwing any
 * {@link RuntimeException} from it causes {@link ParsedArgs#get} to report the failure.
 */
record ArgDef<T>(String name, Class<T> type, Function<String, T> parse, boolean required, T defaultValue) {

    Object applyParse(String raw) {
        return parse.apply(raw);
    }
}
