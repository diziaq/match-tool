package org.example.cli;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Reads raw parameter values from a {@link Properties} object or a {@code .properties} file.
 * Property keys become parameter names; property values become raw strings.
 */
public final class PropertiesArgSource implements ArgSource {

    private final Properties properties;

    public PropertiesArgSource(Properties properties) {
        this.properties = new Properties();
        this.properties.putAll(properties);
    }

    /** Loads properties from a file path. */
    public static PropertiesArgSource fromFile(Path file) throws IOException {
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            props.load(reader);
        }
        return new PropertiesArgSource(props);
    }

    @Override
    public Map<String, String> load() {
        Map<String, String> map = new LinkedHashMap<>();
        for (String key : properties.stringPropertyNames()) {
            map.put(key, properties.getProperty(key));
        }
        return map;
    }
}
