package org.example.wifi;

/**
 * Immutable value object representing a visible WiFi network.
 *
 * <p>Signal strength is classified into one of three {@link Strength} variants by the
 * {@link #of(String, int)} factory method. The classification rules are built into that factory
 * and are not visible to callers — use {@link #strength()} to inspect the result and
 * {@link #isAccessible()} to filter out {@link Strength.Weak} networks.
 *
 * <p>Natural ordering is case-insensitive by {@code ssid}, consistent with the deduplication
 * logic in {@code NetworkOutputParser.deduplicated()}.
 */
public record Network(String ssid, Strength strength) implements Comparable<Network> {

    private static final int DBM_BEST_THRESHOLD   = -67;
    private static final int DBM_NORMAL_THRESHOLD = -80;
    private static final int PCT_BEST_THRESHOLD   = 70;
    private static final int PCT_NORMAL_THRESHOLD = 40;

    /**
     * Creates a {@link Network} and classifies its signal as {@link Strength.Best},
     * {@link Strength.Normal}, or {@link Strength.Weak}.
     *
     * <p>The unit is inferred from the sign of {@code signal}: negative values are treated as
     * dBm; non-negative values as a 0-100 percent scale.
     *
     * <pre>
     *   dBm  :  signal ≥ -67  → Best  |  signal ≥ -80  → Normal  |  signal &lt; -80  → Weak
     *   %    :  signal ≥  70  → Best  |  signal ≥  40  → Normal  |  signal &lt;  40  → Weak
     * </pre>
     */
    public static Network of(String ssid, int signal) {
        Strength strength = signal < 0 ? classifyDbm(signal) : classifyPercent(signal);
        return new Network(ssid, strength);
    }

    /**
     * Creates a {@link Network} for cases where the actual signal is unknown (e.g. an SSID
     * read from a target file). The network is treated as accessible ({@link Strength.Normal}).
     */
    public static Network of(String ssid) {
        return new Network(ssid, new Strength.Normal(0));
    }

    /**
     * Returns {@code true} if this network has sufficient signal to be considered usable.
     * Networks with {@link Strength.Weak} signal are excluded from the accessible count.
     */
    public boolean isAccessible() {
        return !(strength instanceof Strength.Weak);
    }

    @Override
    public int compareTo(Network other) {
        return String.CASE_INSENSITIVE_ORDER.compare(this.ssid, other.ssid);
    }

    private static Strength classifyDbm(int signal) {
        if (signal >= DBM_BEST_THRESHOLD)   return new Strength.Best(signal);
        if (signal >= DBM_NORMAL_THRESHOLD) return new Strength.Normal(signal);
        return new Strength.Weak(signal);
    }

    private static Strength classifyPercent(int signal) {
        if (signal >= PCT_BEST_THRESHOLD)   return new Strength.Best(signal);
        if (signal >= PCT_NORMAL_THRESHOLD) return new Strength.Normal(signal);
        return new Strength.Weak(signal);
    }
}
