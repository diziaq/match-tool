package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import org.example.cli.ArgSchema;
import org.example.cli.CliArgSource;
import org.example.cli.ParsedArgs;
import org.example.cli.PropertiesArgSource;
import org.example.plugin.Plugin;

/**
 * Application entry point. The command line carries only two things: which plugin to run and where
 * its config file is.
 *
 * <pre>
 *   --plugin &lt;name&gt; --config &lt;path-to.properties&gt;
 * </pre>
 *
 * Everything else — modes, inputs, flags — lives in the plugin's config file (a {@code .properties}
 * file, by convention under {@code input/}). Plugins are discovered through {@link ServiceLoader}, so
 * the entry point depends on no specific plugin.
 *
 * <p><b>Responsibilities.</b> The harness here is responsible for config <em>availability and
 * sanity</em>: it selects the plugin, and loads a config file that exists, is readable, and parses as
 * properties. The <em>plugin</em> is responsible for the config's <em>integrity</em>: its own schema
 * (required keys, types) and semantic checks (for example that an input file exists).
 *
 * <p>This is the only place that calls {@link System#exit}: it maps a usage error to exit code 1.
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

        // The command line carries only --plugin and --config.
        ArgSchema bootstrap = ArgSchema.builder()
            .required("plugin", String.class, s -> s)
            .required("config", Path.class, Path::of)
            .build();
        ParsedArgs cli = new ParsedArgs(bootstrap, new CliArgSource(args));
        String pluginName = cli.get("plugin");
        Path configPath = cli.get("config");

        Plugin plugin = plugins.get(pluginName);
        if (plugin == null) {
            throw new IllegalArgumentException(
                "unknown plugin '" + pluginName + "'. Available: " + plugins.keySet());
        }

        // Harness responsibility: the config is available and sane (exists, readable, parses).
        if (!Files.isReadable(configPath)) {
            throw new IllegalArgumentException("config file not found or not readable: " + configPath);
        }
        PropertiesArgSource config;
        try {
            config = PropertiesArgSource.fromFile(configPath);
        } catch (IOException e) {
            throw new IllegalArgumentException("cannot read config file " + configPath + ": " + e.getMessage());
        }

        // Plugin responsibility: validate the config's integrity, then run.
        plugin.run(new ParsedArgs(plugin.schema(), config));
    }

    private static Map<String, Plugin> discoverPlugins() {
        Map<String, Plugin> plugins = new LinkedHashMap<>();
        for (Plugin plugin : ServiceLoader.load(Plugin.class)) {
            plugins.put(plugin.name(), plugin);
        }
        return plugins;
    }
}
