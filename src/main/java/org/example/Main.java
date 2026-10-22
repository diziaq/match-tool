package org.example;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import org.example.cli.ArgSchema;
import org.example.cli.CliArgSource;
import org.example.cli.ParsedArgs;
import org.example.plugin.Plugin;

/**
 * Application entry point. Selects a plugin by name with {@code --plugin <name>} and runs it; the
 * plugin declares and parses its own arguments. Plugins are discovered through {@link ServiceLoader},
 * so the entry point depends on no specific plugin — the WiFi behaviour lives entirely in its plugin.
 *
 * <pre>
 *   --plugin wifi --mode 1
 *   --plugin wifi --mode 2 --networks &lt;file&gt; --passwords &lt;file&gt; [--skip N] [--debug true]
 * </pre>
 *
 * <p>This is the only place that calls {@link System#exit}: it maps a usage error or a run failure to
 * exit code 1. Plugins signal a usage error by throwing {@link IllegalArgumentException}.
 */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        try {
            run(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Usage error:\n" + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void run(String[] args) throws Exception {
        Map<String, Plugin> plugins = discoverPlugins();
        Map<String, String> raw = new CliArgSource(args).load();

        ArgSchema bootstrap = ArgSchema.builder()
            .required("plugin", String.class, s -> s)
            .build();
        String pluginName = new ParsedArgs(bootstrap, raw).get("plugin");

        Plugin plugin = plugins.get(pluginName);
        if (plugin == null) {
            throw new IllegalArgumentException(
                "unknown plugin '" + pluginName + "'. Available: " + plugins.keySet());
        }

        plugin.run(new ParsedArgs(plugin.schema(), raw));
    }

    private static Map<String, Plugin> discoverPlugins() {
        Map<String, Plugin> plugins = new LinkedHashMap<>();
        for (Plugin plugin : ServiceLoader.load(Plugin.class)) {
            plugins.put(plugin.name(), plugin);
        }
        return plugins;
    }
}
