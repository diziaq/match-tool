package org.example.plugin;

import org.example.cli.ArgSchema;
import org.example.cli.ParsedArgs;

/**
 * A pluggable application built on the {@link org.example.engine.PairingEngine}. Each plugin names
 * itself, declares the command-line arguments it accepts, and runs. Plugins are discovered at
 * startup through {@link java.util.ServiceLoader}; the engine knows nothing about any specific
 * plugin.
 *
 * <p>Register a plugin by listing its fully-qualified class name in
 * {@code META-INF/services/org.example.plugin.Plugin}.
 */
public interface Plugin {

    /** The name used to select this plugin on the command line ({@code --plugin <name>}). */
    String name();

    /** The argument schema this plugin accepts, used to parse the command line. */
    ArgSchema schema();

    /**
     * How many pair evaluations the engine may run at once for this plugin. Declared by the plugin
     * and enforced by the engine. The default is {@code 1} (sequential); a plugin whose work is
     * bound to a single device must keep it at {@code 1} — for example the WiFi plugin, because a
     * laptop has one radio.
     */
    default int parallelismBudget() {
        return 1;
    }

    /**
     * Runs the plugin with the parsed arguments.
     *
     * @throws IllegalArgumentException if a required argument is missing or invalid
     * @throws Exception                if the run fails
     */
    void run(ParsedArgs args) throws Exception;
}
