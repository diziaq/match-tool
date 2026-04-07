package org.example.cli;

import java.util.function.Function;

/**
 * Descriptor for a single registered CLI argument.
 *
 * <p>Instances are created exclusively by {@link ArgsParser#registerRequired} and
 * {@link ArgsParser#registerOptional}. The {@code parse} function converts the raw string value
 * from the command line to the typed result; throwing any {@link RuntimeException} from it causes
 * the parser to record an error and continue collecting further failures.
 */
record ArgDef<T>(String name, Class<T> type, Function<String, T> parse, boolean required, T defaultValue) {

    Object applyParse(String raw) {
        return parse.apply(raw);
    }
}
