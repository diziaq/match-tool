package org.example.wifi.connect;

/**
 * Classifies {@code nmcli} output into typed {@link ConnectOutcome} values.
 *
 * <p>nmcli prints "successfully" (case-insensitive) anywhere in the output on success, so a
 * case-insensitive substring check is sufficient. Everything else is an
 * {@link ConnectOutcome.UnknownFailure}; Linux nmcli does not expose the same granular error codes
 * as macOS {@code networksetup}, so {@link ConnectOutcome.WrongPassword} and
 * {@link ConnectOutcome.AssociationFailed} are not produced on this platform.
 */
class LinuxOutputClassifier implements OutputClassifier {

    @Override
    public ConnectOutcome classify(String output) {
        return output.toLowerCase().contains("successfully")
            ? new ConnectOutcome.Connected()
            : new ConnectOutcome.UnknownFailure(output);
    }
}
