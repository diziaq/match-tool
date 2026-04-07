package org.example.matcher;

/**
 * Functional interface for testing whether a left/right pair constitutes a match.
 *
 * <p>Unlike a plain {@code BiFunction<L, R, Boolean>}, this interface returns a typed
 * {@link MatchOutcome}, allowing callers to distinguish a definitive {@link MatchOutcome.Mismatch}
 * from an {@link MatchOutcome.Unavailable} target or a technical {@link MatchOutcome.Failure} —
 * three outcomes that all mean "not a match" but carry different signals for the calling loop.
 *
 * <p>Domain-specific connectors (e.g. {@code NetworkConnector}) extend this interface with
 * concrete type parameters so they can be passed directly as predicates to {@link PairMatcher}.
 *
 * @param <L> the type of the left value (e.g. a network to connect to)
 * @param <R> the type of the right value (e.g. a candidate password)
 */
@FunctionalInterface
public interface MatchPredicate<L, R> {

    /**
     * Tests whether {@code left} and {@code right} constitute a match.
     *
     * @return a non-null {@link MatchOutcome}; {@link MatchOutcome.Match} signals success,
     *         all other variants signal non-match with varying semantics
     */
    MatchOutcome test(L left, R right);
}
