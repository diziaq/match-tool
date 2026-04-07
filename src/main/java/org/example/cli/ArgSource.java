package org.example.cli;

import java.util.Map;

/**
 * Pluggable provider of raw parameter values. Each implementation reads from a different
 * source (CLI arguments, a {@link Map}, a properties file, etc.) and produces a flat
 * name→value map that {@link ParsedArgs} evaluates lazily.
 *
 * <p>Implementations may throw unchecked exceptions if the source data is structurally
 * malformed (e.g., a CLI array that cannot be tokenised into {@code --name value} pairs).
 */
public interface ArgSource {

    /** Returns raw string values keyed by parameter name. */
    Map<String, String> load();
}
