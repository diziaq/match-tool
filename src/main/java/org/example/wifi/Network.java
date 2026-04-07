package org.example.wifi;

/**
 * Immutable value object representing a visible WiFi network.
 *
 * <p>{@code signal} is stored as a formatted string (e.g. {@code "-45 dBm"} on macOS,
 * {@code "75%"} on Linux) produced by {@link org.example.wifi.scan.NetworkOutputParser}.
 * Natural ordering is case-insensitive by {@code ssid}, consistent with the deduplication logic
 * in {@code NetworkOutputParser.deduplicated()}.
 */
public record Network(String ssid, String signal) implements Comparable<Network> {
    @Override
    public int compareTo(Network other) {
        return String.CASE_INSENSITIVE_ORDER.compare(this.ssid, other.ssid);
    }
}
