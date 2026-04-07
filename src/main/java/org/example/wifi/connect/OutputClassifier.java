package org.example.wifi.connect;

import org.example.Platform;
import org.example.PlatformDependent;
import org.example.matcher.MatchOutcome;

/**
 * Interprets the raw stdout of a connection command as a typed {@link MatchOutcome}.
 *
 * <p>Each OS platform has its own classifier because the tool output format differs:
 * macOS uses {@code networksetup} error codes and message text, while Linux uses {@code nmcli}'s
 * "successfully connected" phrasing.
 */
@FunctionalInterface
public interface OutputClassifier extends PlatformDependent {
    /**
     * @param output the raw stdout returned by the connection command (may be blank)
     * @return a non-null {@link MatchOutcome}; unrecognised output must return
     *         {@link MatchOutcome.Failure}
     */
    MatchOutcome classify(String output);

    static OutputClassifier of(Platform platform) {
        return switch (platform) {
            case MACOS -> new MacOsOutputClassifier();
            case LINUX -> new LinuxOutputClassifier();
        };
    }
}
