package org.example.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Builder-style parser for {@code --name value} command-line arguments.
 *
 * <pre>{@code
 * ParsedArgs args = new ArgsParser()
 *     .registerRequired("input", Path.class,    ArgsParser.PATH)
 *     .registerOptional("count", Integer.class, ArgsParser.INTEGER, 10)
 *     .parse(argv);
 *
 * Path  input = args.get("input");
 * int   count = args.get("count");
 * }</pre>
 *
 * All per-argument failures are collected and reported together in {@link ParseException}.
 */
public final class ArgsParser {

    /** Parser for absolute/relative {@link Path} values; resolves relative paths against the jar directory and verifies existence. */
    public static final Function<String, Path>    PATH    = new PathParser();
    /** Parser for {@link Integer} values. */
    public static final Function<String, Integer> INTEGER = new IntegerParser();

    /** Thrown when {@link #parse} encounters one or more errors; the message lists all failures. */
    public static final class ParseException extends Exception {
        ParseException(String message) {
            super(message);
        }
    }

    private final Map<String, ArgDef<?>> defs = new LinkedHashMap<>();

    /** Registers a mandatory parameter. {@link #parse} fails if it is absent or unparseable. */
    public <T> ArgsParser registerRequired(String name, Class<T> type, Function<String, T> parse) {
        defs.put(name, new ArgDef<>(name, type, parse, true, null));
        return this;
    }

    /** Registers an optional parameter. {@link #parse} returns {@code defaultValue} if it is absent. */
    public <T> ArgsParser registerOptional(String name, Class<T> type, Function<String, T> parse, T defaultValue) {
        defs.put(name, new ArgDef<>(name, type, parse, false, defaultValue));
        return this;
    }

    /** Parses {@code args}, collecting all failures before throwing. */
    public ParsedArgs parse(String[] args) throws ParseException {
        List<String> errors = new ArrayList<>();

        Map<String, String> raw = tokenize(args, errors);

        for (String name : raw.keySet()) {
            if (!defs.containsKey(name)) {
                errors.add("--" + name + ": unknown parameter");
            }
        }

        Map<String, Object> values = new LinkedHashMap<>();
        for (var entry : defs.entrySet()) {
            String   name = entry.getKey();
            ArgDef<?> def = entry.getValue();

            if (raw.containsKey(name)) {
                try {
                    values.put(name, def.applyParse(raw.get(name)));
                } catch (Exception e) {
                    errors.add("--" + name + " (" + def.type().getSimpleName() + "): " + e.getMessage());
                }
            } else if (def.required()) {
                errors.add("--" + name + ": required but not provided");
            } else {
                values.put(name, def.defaultValue());
            }
        }

        if (!errors.isEmpty()) {
            throw new ParseException(String.join("\n", errors));
        }

        return new ParsedArgs(values, defs.keySet());
    }

    private static Map<String, String> tokenize(String[] args, List<String> errors) {
        Map<String, String> raw = new LinkedHashMap<>();
        int i = 0;
        while (i < args.length) {
            String token = args[i];
            if (!token.startsWith("--")) {
                errors.add("unexpected token: " + token);
                i++;
                continue;
            }
            String name = token.substring(2);
            if (name.isBlank()) {
                errors.add("empty parameter name after '--'");
                i++;
                continue;
            }
            if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                errors.add("--" + name + ": missing value");
                i++;
                continue;
            }
            if (raw.containsKey(name)) {
                errors.add("--" + name + ": duplicate parameter");
                i += 2;
                continue;
            }
            raw.put(name, args[i + 1]);
            i += 2;
        }
        return raw;
    }
}
