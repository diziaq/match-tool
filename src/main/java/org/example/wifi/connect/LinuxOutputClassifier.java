package org.example.wifi.connect;

import org.example.matcher.MatchOutcome;

/**
 * Classifies {@code nmcli} output into typed {@link MatchOutcome} values.
 *
 * <p>nmcli prints "successfully" (case-insensitive) anywhere in the output on success.
 * Everything else is a {@link MatchOutcome.Failure} because nmcli does not expose granular error
 * codes equivalent to macOS {@code networksetup} — wrong password, association failure, and
 * unknown errors all produce non-"successfully" output without further distinction.
 * {@link MatchOutcome.Unavailable} is not produced on this platform for the same reason.
 */
class LinuxOutputClassifier implements OutputClassifier {

    @Override
    public MatchOutcome classify(String output) {
        return output.toLowerCase().contains("successfully")
            ? new MatchOutcome.Match()
            : new MatchOutcome.Failure(output);
    }
}
