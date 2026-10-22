package org.example.wifi.connect;

import org.example.engine.Outcome;

/**
 * Classifies {@code nmcli} output into typed {@link Outcome} values.
 *
 * <p>nmcli prints "successfully" (case-insensitive) anywhere in the output on success.
 * Everything else is a {@link Outcome.Failure} because nmcli does not expose granular error
 * codes equivalent to macOS {@code networksetup} — wrong password, association failure, and
 * unknown errors all produce non-"successfully" output without further distinction.
 * {@link Outcome.Skip} is not produced on this platform for the same reason.
 */
class LinuxOutputClassifier implements OutputClassifier {

    @Override
    public Outcome classify(String output) {
        return output.toLowerCase().contains("successfully")
            ? new Outcome.Match()
            : new Outcome.Failure(output);
    }
}
