package org.example.cli;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Lazy-evaluating typed result that combines an {@link ArgSchema} (the "intention" config)
 * with raw values from an {@link ArgSource}.
 *
 * <p>Raw strings are captured at construction time. Each parameter is resolved (parsed + cached)
 * on its first {@link #get} call. If the application never requests a malformed or absent
 * parameter, no error is raised — fail-on-first-access semantics.
 *
 * <p>When resolution fails, the exception message includes a usage guide listing every
 * registered parameter with its type and required/optional status.
 */
public final class ParsedArgs {

    private static final Logger LOG = Logger.getLogger(ParsedArgs.class.getName());

    private final Map<String, ArgDef<?>> defs;
    private final Map<String, String>    raw;
    private final Map<String, Object>    resolved = new LinkedHashMap<>();
    private final Map<String, Boolean>   attempted = new LinkedHashMap<>();

    /** Creates a lazy args container from a schema and a pre-loaded raw value map. */
    public ParsedArgs(ArgSchema schema, Map<String, String> raw) {
        this.defs = schema.defs();
        this.raw  = Map.copyOf(raw);
        if (LOG.isLoggable(Level.FINE)) {
            this.raw.forEach((k, v) -> LOG.fine("raw arg --" + k + " = " + v));
        }
    }

    /** Creates a lazy args container from a schema and an arg source (loads immediately). */
    public ParsedArgs(ArgSchema schema, ArgSource source) {
        this(schema, source.load());
    }

    /**
     * Returns the resolved value for the given parameter name.
     *
     * <p>On first access the raw string is passed through the registered parser and the result
     * is cached. Subsequent calls return the cached value. If the parameter was not provided
     * and has a default, the default is returned. If it was required but absent, an error
     * is raised.
     *
     * @throws IllegalArgumentException if {@code name} was not registered, if a required
     *         parameter is missing, or if the raw value cannot be parsed — the message
     *         includes a usage guide
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String name) {
        if (!defs.containsKey(name)) {
            throw new IllegalArgumentException(
                "Unknown parameter: --" + name + "\n\n" + usageGuide());
        }

        if (attempted.containsKey(name)) {
            return (T) resolved.get(name);
        }

        ArgDef<?> def = defs.get(name);
        attempted.put(name, true);

        if (raw.containsKey(name)) {
            try {
                Object value = def.applyParse(raw.get(name));
                resolved.put(name, value);
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("resolved --" + name + " = " + value);
                }
                return (T) value;
            } catch (Exception e) {
                throw new IllegalArgumentException(
                    "--" + name + " (" + def.type().getSimpleName() + "): "
                        + e.getMessage() + "\n\n" + usageGuide());
            }
        }

        if (def.required()) {
            throw new IllegalArgumentException(
                "--" + name + ": required but not provided\n\n" + usageGuide());
        }

        Object defaultValue = def.defaultValue();
        resolved.put(name, defaultValue);
        return (T) defaultValue;
    }

    /**
     * Builds a help guide listing every registered parameter, its type, and whether it is
     * required or optional (with its default value).
     */
    private String usageGuide() {
        String params = defs.values().stream()
            .map(def -> {
                String req = def.required() ? "required" : "optional, default=" + def.defaultValue();
                return "  --" + def.name() + "  (" + def.type().getSimpleName() + ")  [" + req + "]";
            })
            .collect(Collectors.joining("\n"));
        return "Available parameters:\n" + params;
    }
}
