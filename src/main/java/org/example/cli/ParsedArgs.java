package org.example.cli;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Lazy-evaluating typed result of a successful {@link ArgsParser#parse} call.
 *
 * <p>Raw argument strings and their {@link ArgDef} definitions (the "intention" config) are
 * captured at construction time. Each parameter is resolved (parsed + cached) on its first
 * {@link #get} call. If the application never requests a malformed or absent optional parameter,
 * no error is raised — fail-on-first-access semantics.
 *
 * <p>When resolution fails, the exception message includes a help guide listing every registered
 * parameter with its type and required/optional status.
 */
public final class ParsedArgs {

    private static final Logger LOG = Logger.getLogger(ParsedArgs.class.getName());

    private final Map<String, String>    raw;
    private final Map<String, ArgDef<?>> defs;
    private final Map<String, Object>    resolved = new LinkedHashMap<>();
    private final Map<String, Boolean>   attempted = new LinkedHashMap<>();

    ParsedArgs(Map<String, String> raw, Map<String, ArgDef<?>> defs) {
        this.raw  = Map.copyOf(raw);
        this.defs = new LinkedHashMap<>(defs);
        if (LOG.isLoggable(Level.FINE)) {
            this.raw.forEach((k, v) -> LOG.fine("raw arg --" + k + " = " + v));
        }
    }

    /**
     * Returns the resolved value for the given parameter name.
     *
     * <p>On first access the raw string is passed through the registered parser and the result is
     * cached. Subsequent calls return the cached value. If the parameter was not provided and has a
     * default, the default is returned immediately.
     *
     * @throws IllegalArgumentException if {@code name} was not registered, or if the raw value
     *                                  cannot be parsed — the message includes a usage guide
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

        // Not provided — use default (required-but-missing is caught by ArgsParser.parse)
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
