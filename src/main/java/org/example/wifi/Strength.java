package org.example.wifi;

/**
 * Sealed hierarchy representing the signal strength of a WiFi network.
 * Each variant carries the raw integer signal value and is classified by
 * {@link Network#of(String, int)}.
 *
 * <p>Classification thresholds (auto-detected by sign of the raw value):
 * <pre>
 *   dBm (negative) :  ≥ -67  → Best  |  ≥ -80  → Normal  |  &lt; -80  → Weak
 *   %   (positive) :  ≥  70  → Best  |  ≥  40  → Normal  |  &lt;  40  → Weak
 * </pre>
 *
 * <p>Only {@link Weak} networks are excluded from the accessible-network count;
 * see {@link Network#isAccessible()}.
 */
public sealed interface Strength permits Strength.Best, Strength.Normal, Strength.Weak {

    /** The raw signal integer — negative dBm or 0-100 %. */
    int value();

    /** Strong signal — reliable for high-bandwidth use. */
    record Best(int value) implements Strength {
        @Override public String toString() { return "Best(" + value + ")"; }
    }

    /** Adequate signal — suitable for normal use. */
    record Normal(int value) implements Strength {
        @Override public String toString() { return "Normal(" + value + ")"; }
    }

    /** Poor signal — connectivity unreliable; excluded from the accessible-network count. */
    record Weak(int value) implements Strength {
        @Override public String toString() { return "Weak(" + value + ")"; }
    }
}
