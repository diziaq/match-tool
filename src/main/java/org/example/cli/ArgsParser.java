package org.example.cli;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses and validates {@code --name value} style command-line arguments
 * against a fixed set of parameter specifications provided at construction.
 *
 * <pre>{@code
 * ArgsParser parser = new ArgsParser(Map.of(
 *     "input", ParamSpec.of(ParamType.PATH)
 *                       .required()
 *                       .validatedBy(ParamSpec.existingNonEmptyFile(), "must be an existing non-empty file"),
 *     "count", ParamSpec.of(ParamType.INTEGER)
 * ));
 *
 * ParsedArgs args = parser.parse(new String[]{"--input", "/data/file.csv", "--count", "10"});
 * Path input  = args.get("input");
 * Optional<Integer> count = args.getOptional("count");
 * }</pre>
 */
public final class ArgsParser {

    private final Map<String, ParamSpec<?>> specs;

    public ArgsParser(Map<String, ParamSpec<?>> specs) {
        this.specs = Map.copyOf(specs);
    }

    public ParsedArgs parse(String[] args) throws ArgsParseException {
        Map<String, String> raw = extractRawPairs(args);
        checkForUnknown(raw);
        checkForRequired(raw);
        return new ParsedArgs(convertAll(raw));
    }

    private Map<String, String> extractRawPairs(String[] args) throws ArgsParseException {
        Map<String, String> raw = new LinkedHashMap<>();
        int i = 0;
        while (i < args.length) {
            String token = args[i];
            if (!token.startsWith("--")) {
                throw new ArgsParseException("Expected --name, got: " + token);
            }
            String name = token.substring(2);
            if (name.isBlank()) {
                throw new ArgsParseException("Empty parameter name after '--'");
            }
            if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                throw new ArgsParseException("Missing value for --" + name);
            }
            if (raw.containsKey(name)) {
                throw new ArgsParseException("Duplicate parameter: --" + name);
            }
            raw.put(name, args[i + 1]);
            i += 2;
        }
        return raw;
    }

    private void checkForUnknown(Map<String, String> raw) throws ArgsParseException {
        for (String name : raw.keySet()) {
            if (!specs.containsKey(name)) {
                throw new ArgsParseException("Unknown parameter: --" + name);
            }
        }
    }

    private void checkForRequired(Map<String, String> raw) throws ArgsParseException {
        for (var entry : specs.entrySet()) {
            if (entry.getValue().isRequired() && !raw.containsKey(entry.getKey())) {
                throw new ArgsParseException("Missing required parameter: --" + entry.getKey());
            }
        }
    }

    private Map<String, Object> convertAll(Map<String, String> raw) throws ArgsParseException {
        Map<String, Object> result = new LinkedHashMap<>();
        for (var entry : raw.entrySet()) {
            result.put(entry.getKey(), convertAndValidate(entry.getKey(), entry.getValue(), specs.get(entry.getKey())));
        }
        return result;
    }

    private <T> T convertAndValidate(String name, String raw, ParamSpec<T> spec) throws ArgsParseException {
        T value;
        try {
            value = spec.type().convert(raw);
        } catch (Exception e) {
            throw new ArgsParseException(
                "Invalid value for --" + name + " (expected " + spec.type().name() + "): " + e.getMessage()
            );
        }
        if (spec.validator() != null && !spec.validator().test(value)) {
            throw new ArgsParseException("Validation failed for --" + name + ": " + spec.validationMessage());
        }
        return value;
    }
}
