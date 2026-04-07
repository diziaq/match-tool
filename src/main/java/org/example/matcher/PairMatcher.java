package org.example.matcher;

import java.util.Collection;
import java.util.function.BiFunction;
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
 * new PairMatcher<String, String>().match(
 *     ssids,
 *     Files.lines(passwordFile),
 *     (ssid, password) -> connector.tryConnect(ssid, password) instanceof Connected,
 *     match -> log.info("Found: " + match)
 * );
 * }</pre>
 */
public class PairMatcher<L, R> {

    /**
     * For every {@code right} in {@code rights}, tests it against every {@code left} in
     * {@code lefts}. When {@code predicate} returns {@code true}, calls {@code onMatch} with the
     * matching pair.
     *
     * @param lefts     the collection of left items (iterated once per right)
     * @param rights    the stream of right items (consumed exactly once)
     * @param predicate returns {@code true} when a pair should be reported
     * @param onMatch   called for every pair where the predicate is satisfied
     */
    public void match(
        Collection<L> lefts,
        Stream<R> rights,
        BiFunction<L, R, Boolean> predicate,
        Consumer<Match<L, R>> onMatch
    ) {
        rights.forEach(right ->
            lefts.forEach(left -> {
                if (predicate.apply(left, right)) {
                    onMatch.accept(new Match<>(left, right));
                }
            })
        );
    }
}
