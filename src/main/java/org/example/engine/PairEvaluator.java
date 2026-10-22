package org.example.engine;

/**
 * Evaluates one pair of items and returns a typed {@link Outcome}. This is the pluggable "action"
 * the {@link PairingEngine} runs for every pair.
 *
 * <p>It returns an {@link Outcome} rather than a boolean so the engine can tell a definitive
 * {@link Outcome.Mismatch} from a dead-end {@link Outcome.Skip} or a retryable
 * {@link Outcome.Failure}. The side effect for an accepted pair is handled separately by the
 * engine's result consumer, keeping evaluation pure.
 *
 * <p>Domain plugins extend this with concrete types (for example a WiFi connector is a
 * {@code PairEvaluator<Network, Password>}).
 *
 * @param <A> the first item type (the evaluator's first argument)
 * @param <B> the second item type (the evaluator's second argument)
 */
@FunctionalInterface
public interface PairEvaluator<A, B> {

    /**
     * Evaluates {@code a} against {@code b}.
     *
     * @return a non-null {@link Outcome}; {@link Outcome.Match} signals an accepted pair
     */
    Outcome evaluate(A a, B b);
}
