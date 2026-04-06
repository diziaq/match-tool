package org.example.cli;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Immutable typed result of a successful {@link ArgsParser#parse} call. */
public final class ParsedArgs {

    private static final Logger LOG = Logger.getLogger(ParsedArgs.class.getName());

    private final Map<String, Object> values;
    private final Set<String> knownNames;

    ParsedArgs(Map<String, Object> values, Set<String> knownNames) {
        this.values     = Collections.unmodifiableMap(new LinkedHashMap<>(values));
        this.knownNames = Set.copyOf(knownNames);
        if (LOG.isLoggable(Level.FINE)) {
            this.values.forEach((k, v) -> LOG.fine("arg --" + k + " = " + v));
        }
    }

    /**
     * Returns the value for the given parameter name.
     *
     * @throws IllegalArgumentException if {@code name} was not registered with the parser
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String name) {
        if (!knownNames.contains(name)) {
            throw new IllegalArgumentException("Unknown parameter: --" + name);
        }
        return (T) values.get(name);
    }
}
