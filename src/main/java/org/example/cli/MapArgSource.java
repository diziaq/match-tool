package org.example.cli;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Wraps an existing {@link Map Map&lt;String, String&gt;} as an {@link ArgSource}.
 * A defensive copy is taken at construction time.
 */
public final class MapArgSource implements ArgSource {

    private final Map<String, String> values;

    public MapArgSource(Map<String, String> values) {
        this.values = new LinkedHashMap<>(values);
    }

    @Override
    public Map<String, String> load() {
        return new LinkedHashMap<>(values);
    }
}
