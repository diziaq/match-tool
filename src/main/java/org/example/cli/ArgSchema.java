package org.example.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Declares the "intention" config — every parameter's name, type, parser, required/optional
 * status, and default value. Constructed via {@link #builder()} (Java API) or
 * {@link #fromText(String)} (declarative text format).
 *
 * <p>Built-in type parsers are exposed as constants: {@link #PATH}, {@link #INTEGER}.
 */
public final class ArgSchema {

    /** Parser for absolute/relative {@link Path} values; resolves relative paths against the jar directory and verifies existence. */
    public static final Function<String, Path>    PATH    = new PathParser();
    /** Parser for {@link Integer} values. */
    public static final Function<String, Integer> INTEGER = new IntegerParser();

    private final Map<String, ArgDef<?>> defs;

    private ArgSchema(Map<String, ArgDef<?>> defs) {
        this.defs = Collections.unmodifiableMap(new LinkedHashMap<>(defs));
    }

    /** Package-private access to definitions for {@link ParsedArgs}. */
    Map<String, ArgDef<?>> defs() {
        return defs;
    }

    /** Returns true if a parameter with this name is declared. */
    public boolean contains(String name) {
        return defs.containsKey(name);
    }

    /** Returns the declared parameter names in registration order. */
    public Set<String> names() {
        return defs.keySet();
    }

    // ── Java builder ──────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final Map<String, ArgDef<?>> defs = new LinkedHashMap<>();

        private Builder() {}

        public <T> Builder required(String name, Class<T> type, Function<String, T> parse) {
            defs.put(name, new ArgDef<>(name, type, parse, true, null));
            return this;
        }

        public <T> Builder optional(String name, Class<T> type, Function<String, T> parse, T defaultValue) {
            defs.put(name, new ArgDef<>(name, type, parse, false, defaultValue));
            return this;
        }

        public ArgSchema build() {
            return new ArgSchema(defs);
        }
    }

    // ── Declarative text format ───────────────────────────────────
    //
    //   # comment
    //   mode  : Integer : required
    //   left  : Path    : optional
    //   skip  : Integer : optional : 0
    //   debug : Boolean : optional : false
    //
    // Built-in types: Integer, Boolean, String, Path

    private record TypeEntry<T>(Class<T> type, Function<String, T> parser, Function<String, T> defaultParser) {}

    private static final Map<String, TypeEntry<?>> TYPE_REGISTRY = new LinkedHashMap<>();

    static {
        TYPE_REGISTRY.put("Integer", new TypeEntry<>(Integer.class, new IntegerParser(), Integer::parseInt));
        TYPE_REGISTRY.put("Boolean", new TypeEntry<>(Boolean.class, Boolean::parseBoolean, Boolean::parseBoolean));
        TYPE_REGISTRY.put("String",  new TypeEntry<>(String.class, s -> s, s -> s));
        TYPE_REGISTRY.put("Path",    new TypeEntry<>(Path.class, new PathParser(), s -> Path.of(s)));
    }

    /**
     * Parses an {@link ArgSchema} from a declarative text format.
     *
     * <p>Each non-blank, non-comment line declares one parameter:
     * <pre>
     *   name : type : required|optional [: default_value]
     * </pre>
     *
     * @throws IllegalArgumentException if any line is malformed or references an unknown type
     */
    public static ArgSchema fromText(String text) {
        Builder builder = builder();
        int lineNum = 0;
        for (String line : text.split("\n", -1)) {
            lineNum++;
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] parts = line.split(":", -1);
            if (parts.length < 3) {
                throw new IllegalArgumentException(
                    "Line " + lineNum + ": expected at least 'name : type : required|optional', got: " + line);
            }

            String name     = parts[0].trim();
            String typeName = parts[1].trim();
            String req      = parts[2].trim();

            TypeEntry<?> entry = TYPE_REGISTRY.get(typeName);
            if (entry == null) {
                throw new IllegalArgumentException(
                    "Line " + lineNum + ": unknown type '" + typeName
                        + "'. Available: " + TYPE_REGISTRY.keySet());
            }

            switch (req) {
                case "required" -> addRequired(builder, name, entry);
                case "optional" -> {
                    if (parts.length >= 4) {
                        String defaultRaw = parts[3].trim();
                        addOptionalWithDefault(builder, name, entry, defaultRaw);
                    } else {
                        addOptionalNull(builder, name, entry);
                    }
                }
                default -> throw new IllegalArgumentException(
                    "Line " + lineNum + ": expected 'required' or 'optional', got: '" + req + "'");
            }
        }
        return builder.build();
    }

    /**
     * Loads the text from a file, then delegates to {@link #fromText(String)}.
     */
    public static ArgSchema fromText(Path file) throws IOException {
        return fromText(Files.readString(file));
    }

    @SuppressWarnings("unchecked")
    private static <T> void addRequired(Builder builder, String name, TypeEntry<T> entry) {
        builder.required(name, entry.type(), entry.parser());
    }

    @SuppressWarnings("unchecked")
    private static <T> void addOptionalWithDefault(Builder builder, String name, TypeEntry<T> entry, String raw) {
        T defaultValue = entry.defaultParser().apply(raw);
        builder.optional(name, entry.type(), entry.parser(), defaultValue);
    }

    @SuppressWarnings("unchecked")
    private static <T> void addOptionalNull(Builder builder, String name, TypeEntry<T> entry) {
        builder.optional(name, entry.type(), entry.parser(), null);
    }
}
