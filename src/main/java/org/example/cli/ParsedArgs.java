package org.example.cli;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/** Typed result of a successful {@link ArgsParser#parse} call. */
public final class ParsedArgs {

    private final Map<String, Object> values;

    ParsedArgs(Map<String, Object> values) {
        this.values = Map.copyOf(values);
    }

    /**
     * Returns the value for the given parameter name.
     * The cast is safe as long as the caller uses the same type that was declared in the spec.
     *
     * @throws NoSuchElementException if the parameter was not provided (use {@link #getOptional} for optional params)
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String name) {
        if (!values.containsKey(name)) {
            throw new NoSuchElementException("Parameter not present: --" + name);
        }
        return (T) values.get(name);
    }

    /** Returns the value wrapped in Optional, or empty if the parameter was not provided. */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getOptional(String name) {
        return Optional.ofNullable((T) values.get(name));
    }

    public boolean has(String name) {
        return values.containsKey(name);
    }
}
