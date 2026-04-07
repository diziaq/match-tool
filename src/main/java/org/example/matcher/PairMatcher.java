package org.example.matcher;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Cross-product matcher that pairs every left item with every right item and notifies a consumer
 * for each pair that satisfies a predicate.
 *
 * <p>Iteration order: rights is the outer loop, lefts is the inner loop. This means that for each
 * right element all lefts are tested before advancing to the next right — useful when rights come
 * from a stream that should not be revisited (e.g. a lazy password file).
 *
 * <pre>{@code
 * new PairMatcher<Network, String>().match(
 *     networks,
 *     Files.lines(passwordFile),
 *     connector,          // NetworkConnector IS-A MatchPredicate<Network, String>
 *     match -> log.info("Found: " + match)
 * );
 * }</pre>
 */
public class PairMatcher<L, R> {

    /**
     * For every {@code right} in {@code rights}, tests it against every {@code left} in
     * {@code lefts}. When {@code predicate} returns {@link MatchOutcome.Match}, calls
     * {@code onMatch} with the matched pair. All other outcomes are silently skipped.
     *
     * @param lefts     the collection of left items (iterated once per right)
     * @param rights    the stream of right items (consumed exactly once)
     * @param predicate returns a {@link MatchOutcome} for each pair
     * @param onMatch   called for every pair where the predicate returns {@link MatchOutcome.Match}
     */
    public void match(
        Collection<L> lefts,
        Stream<R> rights,
        MatchPredicate<L, R> predicate,
        Consumer<Match<L, R>> onMatch
    ) {
        rights.forEach(right ->
            lefts.forEach(left -> {
                if (predicate.test(left, right) instanceof MatchOutcome.Match) {
                    onMatch.accept(new Match<>(left, right));
                }
            })
        );
    }
}
