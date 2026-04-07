package org.example.wifi.connect;

/**
 * Attempts to connect to a WiFi network and returns a typed outcome.
 *
 * <p>Implementations must never throw; all failures — including unexpected exceptions — must be
 * returned as {@link ConnectOutcome.UnknownFailure}.
 */
public interface NetworkConnector {
    /**
     * @param ssid     the network name to connect to
     * @param password the WPA/WPA2 passphrase (may be empty for open networks)
     * @return a typed {@link ConnectOutcome}; never {@code null}
     */
    ConnectOutcome tryConnect(String ssid, String password);
}
