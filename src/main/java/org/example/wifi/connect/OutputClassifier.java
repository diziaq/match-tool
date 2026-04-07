package org.example.wifi.connect;

import org.example.Platform;
import org.example.PlatformDependent;

/**
 * Interprets the raw stdout of a connection command as a typed {@link ConnectOutcome}.
 *
 * <p>Each OS platform has its own classifier because the tool output format differs:
 * macOS uses {@code networksetup} error codes and message text, while Linux uses {@code nmcli}'s
 * "successfully connected" phrasing.
 */
@FunctionalInterface
public interface OutputClassifier extends PlatformDependent {
    /**
     * @param output the raw stdout returned by the connection command (may be blank)
     * @return a non-null {@link ConnectOutcome}; unrecognised output should return
     *         {@link ConnectOutcome.UnknownFailure}
     */
    ConnectOutcome classify(String output);

    static OutputClassifier of(Platform platform) {
        return switch (platform) {
            case MACOS -> new MacOsOutputClassifier();
            case LINUX -> new LinuxOutputClassifier();
        };
    }
}
