package org.example.wifi.connect;

import org.example.Platform;
import org.example.PlatformDependent;

/**
 * Builds the OS-specific shell command that attempts a WiFi connection.
 *
 * <p>Implementations must shell-escape both {@code ssid} and {@code password} via
 * {@link ShellEscape#quoted} to prevent injection through special characters in network names or
 * passwords.
 */
@FunctionalInterface
public interface ConnectCommand extends PlatformDependent {
    /**
     * @return a fully-formed shell command ready to be passed to {@link org.example.shell.ShellRunner#run}
     */
    String build(String ssid, String password);

    static ConnectCommand of(Platform platform) {
        return switch (platform) {
            case MACOS -> new MacOsConnectCommand();
            case LINUX -> new LinuxConnectCommand();
        };
    }
}
