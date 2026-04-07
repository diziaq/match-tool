package org.example.matcher;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Configurable coordinator for cross-product matching that wraps the core iteration of
 * {@link PairMatcher} with runtime policies for stopping, exclusion, and retry.
 *
 * <p>All policies default to off. Combine them with the fluent builder:
 * <pre>{@code
 * MatchCoordinator.<Network, Password>builder()
 *     .stopLeftOnFirstMatch()
 *     .excludeOnUnavailable()
 *     .excludeAfterFailures(3)
 *     .retries(2)
 *     .build();
 * }</pre>
 *
 * <p>For WiFi scanning use the ready-made {@link #effective()} preset, which applies all of the
 * above with values tuned for batch password-spray with transient-error recovery.
 *
 * <h3>Policy semantics</h3>
 * <ul>
 *   <li><b>stopLeftOnFirstMatch</b> — once a left item produces {@link MatchOutcome.Match}, it is
 *       excluded from all subsequent right items. Use when finding the password for one SSID
 *       should not keep trying more passwords for it.</li>
 *   <li><b>stopAllOnFirstMatch</b> — terminates the entire run on the first {@link MatchOutcome.Match}.
 *       Supersedes {@code stopLeftOnFirstMatch}.</li>
 *   <li><b>excludeOnUnavailable</b> — excludes a left item immediately when the predicate returns
 *       {@link MatchOutcome.Unavailable}. No password will help a network that is not visible.</li>
 *   <li><b>excludeAfterFailures(n)</b> — excludes a left item after it accumulates {@code n}
 *       persistent {@link MatchOutcome.Failure} outcomes (after all retries are exhausted).
 *       Protects against a broken target consuming the entire right stream.</li>
 *   <li><b>retries(n)</b> — when the predicate returns {@link MatchOutcome.Failure}, the same
 *       pair is retried up to {@code n} more times. If any retry succeeds the failure counter is
 *       not incremented. Only retries {@code Failure}; {@code Mismatch} and {@code Unavailable}
 *       are definitive and never retried.</li>
 * </ul>
 *
 * <h3>Iteration contract</h3>
 * Same as {@link PairMatcher}: rights is the outer loop, lefts is the inner loop. The rights
 * stream is consumed at most once. Excluded lefts are skipped on all future right elements.
 */
public final class MatchCoordinator<L, R> {

    private final boolean stopLeftOnFirstMatch;
    private final boolean stopAllOnFirstMatch;
    private final boolean excludeOnUnavailable;
    private final int     excludeAfterFailures; // 0 = disabled
    private final int     maxRetries;           // 0 = no retry

    private MatchCoordinator(Builder<L, R> b) {
        this.stopLeftOnFirstMatch = b.stopLeftOnFirstMatch;
        this.stopAllOnFirstMatch  = b.stopAllOnFirstMatch;
        this.excludeOnUnavailable = b.excludeOnUnavailable;
        this.excludeAfterFailures = b.excludeAfterFailures;
        this.maxRetries           = b.maxRetries;
    }

    /**
     * Returns a pre-configured coordinator suitable for WiFi batch password scanning:
     * <ul>
     *   <li>Stop trying more passwords for an SSID once it connects ({@code stopLeftOnFirstMatch})</li>
     *   <li>Skip networks that are not visible ({@code excludeOnUnavailable})</li>
     *   <li>Skip networks with 3 or more persistent errors ({@code excludeAfterFailures(3)})</li>
     *   <li>Retry each transient error twice before counting it ({@code retries(2)})</li>
     * </ul>
     */
    public static <L, R> MatchCoordinator<L, R> effective() {
        return MatchCoordinator.<L, R>builder()
            .stopLeftOnFirstMatch()
            .excludeOnUnavailable()
            .excludeAfterFailures(3)
            .retries(2)
            .build();
    }

    /** Returns a new builder. */
    public static <L, R> Builder<L, R> builder() {
        return new Builder<>();
    }

    /**
     * Runs the cross-product match with all configured policies applied.
     *
     * @param lefts     the items to test against each right (e.g. target networks)
     * @param rights    the stream of candidates (e.g. passwords); consumed exactly once
     * @param predicate the matching function
     * @param onMatch   called for every pair where the predicate returns {@link MatchOutcome.Match}
     */
    public void match(
        Collection<L> lefts,
        Stream<R> rights,
        MatchPredicate<L, R> predicate,
        Consumer<Match<L, R>> onMatch
    ) {
        Set<L>         excluded      = new HashSet<>();
        Map<L, Integer> failureCounts = new HashMap<>();
        AtomicBoolean  stopped       = new AtomicBoolean(false);

        rights
            .takeWhile(r -> !stopped.get())
            .forEach(right -> {
                for (L left : lefts) {
                    if (stopped.get())         break;
                    if (excluded.contains(left)) continue;

                    MatchOutcome outcome = withRetry(left, right, predicate);

                    switch (outcome) {
                        case MatchOutcome.Match ignored -> {
                            onMatch.accept(new Match<>(left, right));
                            if (stopLeftOnFirstMatch) excluded.add(left);
                            if (stopAllOnFirstMatch)  stopped.set(true);
                        }
                        case MatchOutcome.Unavailable ignored -> {
                            if (excludeOnUnavailable) excluded.add(left);
                        }
                        case MatchOutcome.Failure ignored -> {
                            if (excludeAfterFailures > 0) {
                                int count = failureCounts.merge(left, 1, Integer::sum);
                                if (count >= excludeAfterFailures) excluded.add(left);
                            }
                        }
                        case MatchOutcome.Mismatch ignored -> { /* continue to next right */ }
                    }
                }
            });
    }

    /**
     * Calls the predicate for {@code (left, right)}, retrying up to {@link #maxRetries} additional
     * times while the outcome is {@link MatchOutcome.Failure}. Returns the first non-{@code Failure}
     * outcome, or the last {@code Failure} if all attempts are exhausted.
     */
    private MatchOutcome withRetry(L left, R right, MatchPredicate<L, R> predicate) {
        MatchOutcome outcome = predicate.test(left, right);
        for (int attempt = 0; attempt < maxRetries && outcome instanceof MatchOutcome.Failure; attempt++) {
            outcome = predicate.test(left, right);
        }
        return outcome;
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    /** Fluent builder for {@link MatchCoordinator}. All options default to off / 0. */
    public static final class Builder<L, R> {

        private boolean stopLeftOnFirstMatch = false;
        private boolean stopAllOnFirstMatch  = false;
        private boolean excludeOnUnavailable = false;
        private int     excludeAfterFailures = 0;
        private int     maxRetries           = 0;

        /**
         * Once a left item produces {@link MatchOutcome.Match}, exclude it from all future rights.
         */
        public Builder<L, R> stopLeftOnFirstMatch() {
            this.stopLeftOnFirstMatch = true;
            return this;
        }

        /**
         * Stop the entire run as soon as any pair produces {@link MatchOutcome.Match}.
         * Implies the same behaviour as {@link #stopLeftOnFirstMatch()} for the matched item.
         */
        public Builder<L, R> stopAllOnFirstMatch() {
            this.stopAllOnFirstMatch = true;
            return this;
        }

        /**
         * Exclude a left item immediately when the predicate returns {@link MatchOutcome.Unavailable}.
         */
        public Builder<L, R> excludeOnUnavailable() {
            this.excludeOnUnavailable = true;
            return this;
        }

        /**
         * Exclude a left item after it accumulates {@code n} persistent {@link MatchOutcome.Failure}
         * outcomes (outcomes that remain {@code Failure} after all retries).
         *
         * @param n must be &gt;= 1
         */
        public Builder<L, R> excludeAfterFailures(int n) {
            if (n < 1) throw new IllegalArgumentException("n must be >= 1, got: " + n);
            this.excludeAfterFailures = n;
            return this;
        }

        /**
         * Retry a pair up to {@code n} additional times when the outcome is
         * {@link MatchOutcome.Failure} before reporting the outcome to the coordinator.
         *
         * @param n must be &gt;= 0
         */
        public Builder<L, R> retries(int n) {
            if (n < 0) throw new IllegalArgumentException("n must be >= 0, got: " + n);
            this.maxRetries = n;
            return this;
        }

        /** Builds the immutable {@link MatchCoordinator}. */
        public MatchCoordinator<L, R> build() {
            return new MatchCoordinator<>(this);
        }
    }
}
