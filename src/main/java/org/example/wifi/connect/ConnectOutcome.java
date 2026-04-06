package org.example.wifi.connect;

/**
 * Typed result of a single WiFi connection attempt.
 */
public sealed interface ConnectOutcome
    permits ConnectOutcome.Connected,
            ConnectOutcome.NetworkNotFound,
            ConnectOutcome.WrongPassword,
            ConnectOutcome.AssociationFailed,
            ConnectOutcome.UnknownFailure {

    /** The connection succeeded (blank output from networksetup / "successfully" in nmcli output). */
    record Connected() implements ConnectOutcome {}

    /** The network was not visible at the time of the attempt ("Could not find network …"). */
    record NetworkNotFound() implements ConnectOutcome {}

    /**
     * The network rejected the password — authentication aborted
     * (error -3925, -3958, -3970, -528342014 with "tmpErr" in macOS output).
     */
    record WrongPassword() implements ConnectOutcome {}

    /**
     * The network is visible but association was refused for reasons other than the password —
     * typically a security-type or capability mismatch
     * (error -3912 with "apple80211API" in macOS output).
     */
    record AssociationFailed() implements ConnectOutcome {}

    /** The output did not match any known pattern, or an exception occurred. */
    record UnknownFailure(String rawOutput) implements ConnectOutcome {}
}
