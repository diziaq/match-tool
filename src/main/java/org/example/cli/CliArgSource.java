package org.example.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tokenises a {@code String[]} of {@code --name value} pairs into a raw parameter map.
 *
 * <p>Throws {@link IllegalArgumentException} for hard tokenisation errors: bare tokens (no
 * {@code --}), missing values, empty parameter names, or duplicates. These are source-level
 * failures — the array cannot be converted into a valid map.
 */
public final class CliArgSource implements ArgSource {

    private final String[] args;

    public CliArgSource(String[] args) {
        this.args = args.clone();
    }

    @Override
    public Map<String, String> load() {
        List<String> errors = new ArrayList<>();
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

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }

        return raw;
    }
}
