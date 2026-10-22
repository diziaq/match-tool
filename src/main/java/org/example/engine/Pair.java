package org.example.engine;

/**
 * An accepted pair produced by {@link PairingEngine}: {@code a} from the first source type,
 * {@code b} from the second, in the order the {@link PairEvaluator} sees them — independent of which
 * source the engine iterated as the outer loop.
 *
 * <p>In the WiFi plugin {@code a} is the {@code Network} and {@code b} is the {@code Password}.
 * {@link #toString()} renders as {@code "a <-> b"}.
 */
public record Pair<A, B>(A a, B b) {
    @Override
    public String toString() {
        return a + " <-> " + b;
    }
}
