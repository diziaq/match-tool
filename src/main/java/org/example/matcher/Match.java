package org.example.matcher;

/**
 * A matched pair produced by {@link PairMatcher}.
 *
 * <p>In the WiFi use-case {@code L} is an SSID (left file) and {@code R} is a password (right
 * file), but the record is fully generic. {@link #toString()} renders as {@code "left <-> right"}.
 */
public record Match<L, R>(L left, R right) {
    @Override
    public String toString() {
        return left + " <-> " + right;
    }
}
